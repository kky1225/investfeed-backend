package com.example.investfeed.domain.marketindex.crawler

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.exception.MarketIndexApiException
import com.example.investfeed.domain.marketindex.exception.MarketIndexResponseException
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.global.config.WebClientHttpClientFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.coroutines.cancellation.CancellationException
import java.time.LocalDateTime

@Component
class NaverMarketIndexCrawler(
    @param:Value("\${naver-stock.api-url}")
    private val naverApiUrl: String,
    @param:Value("\${naver-stock.mobile-url}")
    private val naverMobileUrl: String,
    @param:Value("\${naver-stock.pc-url}")
    private val naverPcUrl: String,
    private val objectMapper: ObjectMapper,
    private val apiCallCounterService: ApiCallCounterService,
) {
    private val log = KotlinLogging.logger {}

    private val webClient = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
        .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
        .filter(ExchangeFilterFunction.ofRequestProcessor { req ->
            apiCallCounterService.increment(ApiProvider.NAVER_CRAWL)
            Mono.just(req)
        })
        .build()

    companion object {
        private val WORLD_INDEX_PATHS = mapOf(
            MarketIndexType.NASDAQ to "/index/.IXIC/basic",
            MarketIndexType.SP500 to "/index/.INX/basic",
            MarketIndexType.VIX to "/index/.VIX/basic",
            MarketIndexType.PHILADELPHIA_SEMICONDUCTOR to "/index/.SOX/basic",
        )

        private val DOMESTIC_INDEX_PATHS = mapOf(
            MarketIndexType.KOSPI to "/api/index/KOSPI/basic",
            MarketIndexType.KOSDAQ to "/api/index/KOSDAQ/basic",
        )

        private val EXCHANGE_PATHS = mapOf(
            MarketIndexType.USD_KRW to "/marketindex/exchange/FX_USDKRW",
        )

        private val POLLING_PATHS = mapOf(
            MarketIndexType.WTI to "/api/polling/marketindex/energy/CLcv1",
            MarketIndexType.GOLD_INTERNATIONAL to "/api/polling/marketindex/metals/GCcv1",
            MarketIndexType.DOLLAR_INDEX to "/api/polling/marketindex/exchange/.DXY",
        )
    }

    /**
     * 여러 외부 API를 병렬 호출하여 주요 시장 지수를 수집한다.
     * 개별 엔드포인트 실패는 로깅만 하고 부분 결과를 반환한다(crawler 특성상 partial-fail 허용).
     * 코루틴 경계: 스케줄러(동기)에서 호출되므로 여기서 runBlocking 으로 감싼다.
     */
    fun crawl(): List<MarketIndexRes> = runBlocking { crawlSuspend() }

    private suspend fun crawlSuspend(): List<MarketIndexRes> {
        val result = mutableListOf<MarketIndexRes>()
        val now = LocalDateTime.now()

        val worldApis = WORLD_INDEX_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }
        val domesticApis = DOMESTIC_INDEX_PATHS.map { (type, path) -> type to "$naverMobileUrl$path" }

        result.addAll(fetchAll(worldApis + domesticApis) { type, body -> parseJsonIndex(type, body, now) })
        result.addAll(fetchAll(EXCHANGE_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }) { type, body -> parseExchangeDetail(type, body, now) })
        result.addAll(fetchAll(POLLING_PATHS.map { (type, path) -> type to "$naverPcUrl$path" }) { type, body -> parsePollingIndex(type, body, now) })

        return result
    }

    /** 각 URL 을 동시에 조회해 파싱한다. 실패한 항목만 로그 후 제외하고 나머지는 그대로 반환한다. */
    private suspend fun fetchAll(
        targets: List<Pair<MarketIndexType, String>>,
        parse: (MarketIndexType, String) -> MarketIndexRes,
    ): List<MarketIndexRes> = coroutineScope {
        targets.map { (type, url) -> async { fetchOne(type, url, parse) } }.awaitAll().filterNotNull()
    }

    private suspend fun fetchOne(
        type: MarketIndexType,
        url: String,
        parse: (MarketIndexType, String) -> MarketIndexRes,
    ): MarketIndexRes? {
        return try {
            val body = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("네이버 지수"); Mono.error(MarketIndexApiException()) })
                .awaitBody<String>()
            parse(type, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MarketIndexApiException) {
            log.error { "[${type.displayName}] fetch Error (API): ${e.message}" }
            null
        } catch (e: MarketIndexResponseException) {
            log.error { "[${type.displayName}] fetch Error (Response): ${e.message}" }
            null
        } catch (e: Exception) {
            log.error { "[${type.displayName}] fetch Error: ${e.message}" }
            null
        }
    }

    private fun parseExchangeDetail(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val root = objectMapper.readTree(body)
        val data = root.get("exchangeInfo") ?: throw MarketIndexResponseException()

        val price = data.textOrNull("closePrice") ?: ""
        val changeAmount = data.textOrNull("fluctuations") ?: ""
        val fluctuationsRatio = data.textOrNull("fluctuationsRatio") ?: ""

        val direction = data.path("fluctuationsType").textOrNull("name") ?: ""
        val sign = when (direction) {
            "RISING" -> "+"
            "FALLING" -> "-"
            else -> ""
        }

        val changeAmountWithSign = if (changeAmount.isNotBlank() && !changeAmount.startsWith("+") && !changeAmount.startsWith("-")) {
            "$sign$changeAmount"
        } else {
            changeAmount
        }

        val changeRateWithSign = if (fluctuationsRatio.isNotBlank() && !fluctuationsRatio.startsWith("+") && !fluctuationsRatio.startsWith("-")) {
            "$sign${fluctuationsRatio}%"
        } else if (fluctuationsRatio.isNotBlank()) {
            "${fluctuationsRatio}%"
        } else {
            ""
        }

        val marketStatus = data.textOrNull("marketStatus") ?: ""
        val delayTime = data.path("stockExchangeType").get("delayTime")?.asInt() ?: 0
        val delayStatus = when {
            marketStatus == "CLOSE" -> "장마감"
            delayTime > 0 -> "${delayTime}분 지연"
            else -> "실시간"
        }

        return MarketIndexRes(
            type = type.name,
            name = type.displayName,
            price = price,
            changeAmount = changeAmountWithSign,
            changeRate = changeRateWithSign,
            delayStatus = delayStatus,
            updatedAt = now,
        )
    }

    private fun parsePollingIndex(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val root = objectMapper.readTree(body)
        val data = root.get("datas")?.firstOrNull()
            ?: throw MarketIndexResponseException()

        val price = data.textOrNull("closePrice") ?: ""
        val changeAmount = data.textOrNull("fluctuations") ?: ""
        val fluctuationsRatio = data.textOrNull("fluctuationsRatio") ?: ""

        val direction = data.path("fluctuationsType").textOrNull("name") ?: ""
        val sign = when (direction) {
            "RISING" -> "+"
            "FALLING" -> "-"
            else -> ""
        }

        val changeAmountWithSign = if (changeAmount.isNotBlank() && !changeAmount.startsWith("+") && !changeAmount.startsWith("-")) {
            "$sign$changeAmount"
        } else {
            changeAmount
        }

        val changeRateWithSign = if (fluctuationsRatio.isNotBlank() && !fluctuationsRatio.startsWith("+") && !fluctuationsRatio.startsWith("-")) {
            "$sign${fluctuationsRatio}%"
        } else if (fluctuationsRatio.isNotBlank()) {
            "${fluctuationsRatio}%"
        } else {
            ""
        }

        val marketStatus = data.textOrNull("marketStatus") ?: ""
        val delayTime = data.path("stockExchangeType").get("delayTime")?.asInt() ?: 0
        val delayStatus = when {
            marketStatus == "CLOSE" -> "장마감"
            delayTime > 0 -> "${delayTime}분 지연"
            else -> "실시간"
        }

        return MarketIndexRes(
            type = type.name,
            name = type.displayName,
            price = price,
            changeAmount = changeAmountWithSign,
            changeRate = changeRateWithSign,
            delayStatus = delayStatus,
            updatedAt = now,
        )
    }

    private fun parseJsonIndex(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val node = objectMapper.readTree(body) ?: throw MarketIndexResponseException()

        val price = node.textOrNull("closePrice") ?: ""
        val changeAmount = node.textOrNull("compareToPreviousClosePrice") ?: ""
        val fluctuationsRatio = node.textOrNull("fluctuationsRatio") ?: ""

        val direction = node.path("compareToPreviousPrice").textOrNull("name") ?: ""
        val sign = when (direction) {
            "RISING" -> "+"
            "FALLING" -> "-"
            else -> ""
        }

        val changeAmountWithSign = if (changeAmount.isNotBlank() && !changeAmount.startsWith("+") && !changeAmount.startsWith("-")) {
            "$sign$changeAmount"
        } else {
            changeAmount
        }

        val changeRateWithSign = if (fluctuationsRatio.isNotBlank() && !fluctuationsRatio.startsWith("+") && !fluctuationsRatio.startsWith("-")) {
            "$sign${fluctuationsRatio}%"
        } else if (fluctuationsRatio.isNotBlank()) {
            "${fluctuationsRatio}%"
        } else {
            ""
        }

        val delayTimeName = node.textOrNull("delayTimeName") ?: ""
        val marketStatus = node.textOrNull("marketStatus") ?: ""
        val delayStatus = when {
            marketStatus == "CLOSE" -> "장마감"
            delayTimeName.isNotBlank() -> delayTimeName
            else -> "실시간"
        }

        return MarketIndexRes(
            type = type.name,
            name = type.displayName,
            price = price,
            changeAmount = changeAmountWithSign,
            changeRate = changeRateWithSign,
            delayStatus = delayStatus,
            updatedAt = now,
        )
    }

    private fun JsonNode.textOrNull(fieldName: String): String? {
        val node = this.get(fieldName) ?: return null
        return if (node.isTextual) node.asText() else node.toString()
    }
}
