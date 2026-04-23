package com.example.investfeed.domain.marketindex.crawler

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.exception.MarketIndexApiException
import com.example.investfeed.domain.marketindex.exception.MarketIndexResponseException
import com.example.investfeed.global.config.WebClientHttpClientFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
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
) {
    private val log = KotlinLogging.logger {}

    private val webClient = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(WebClientHttpClientFactory.createDefaultHttpClient()))
        .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .codecs { config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) }
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

        private const val HOME_MAJORS_PATH = "/api/home/majors"
        private val HOME_MAJORS_CODE_MAP = mapOf(
            "FX_USDKRW" to MarketIndexType.USD_KRW,
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
     */
    fun crawl(): List<MarketIndexRes> {
        val result = mutableListOf<MarketIndexRes>()
        val now = LocalDateTime.now()

        val worldApis = WORLD_INDEX_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }
        val domesticApis = DOMESTIC_INDEX_PATHS.map { (type, path) -> type to "$naverMobileUrl$path" }
        val monos = (worldApis + domesticApis).map { (type, url) ->
            fetchJsonIndexMono(type, url, now)
        }

        val jsonResults = Mono.zip(monos) { results ->
            results.filterIsInstance<MarketIndexRes>()
        }.block() ?: emptyList()

        result.addAll(jsonResults)

        try {
            result.addAll(fetchFromHomeMajorsApi(now))
        } catch (e: MarketIndexApiException) {
            log.error { "fetchFromHomeMajorsApi Error (API): ${e.message}" }
        } catch (e: MarketIndexResponseException) {
            log.error { "fetchFromHomeMajorsApi Error (Response): ${e.message}" }
        } catch (e: Exception) {
            log.error { "fetchFromHomeMajorsApi Error: ${e.message}" }
        }

        try {
            result.addAll(fetchFromPollingApi(now))
        } catch (e: MarketIndexApiException) {
            log.error { "fetchFromPollingApi Error (API): ${e.message}" }
        } catch (e: MarketIndexResponseException) {
            log.error { "fetchFromPollingApi Error (Response): ${e.message}" }
        } catch (e: Exception) {
            log.error { "fetchFromPollingApi Error: ${e.message}" }
        }

        return result
    }

    private fun fetchJsonIndexMono(type: MarketIndexType, url: String, now: LocalDateTime): Mono<MarketIndexRes> {
        return webClient.get()
            .uri(url)
            .retrieve()
            .onStatus({ it.isError }, { Mono.error(MarketIndexApiException()) })
            .bodyToMono<String>()
            .map { body -> parseJsonIndex(type, body, now) }
            .onErrorResume { e ->
                log.error { "[${type.displayName}] fetchJsonIndex Error: ${e.message}" }
                Mono.empty()
            }
    }

    private fun fetchFromHomeMajorsApi(now: LocalDateTime): List<MarketIndexRes> {
        try {
            val body = webClient.get()
                .uri("$naverMobileUrl$HOME_MAJORS_PATH")
                .retrieve()
                .onStatus({ it.isError }, { Mono.error(MarketIndexApiException()) })
                .bodyToMono<String>()
                .block() ?: throw MarketIndexResponseException()

            val root = objectMapper.readTree(body)
            val items = root.get("marketIndexInfos") ?: throw MarketIndexResponseException()

            return items.mapNotNull { item ->
                val code = item.get("reutersCode")?.asText() ?: return@mapNotNull null
                val type = HOME_MAJORS_CODE_MAP[code] ?: return@mapNotNull null

                val price = item.textOrNull("closePrice") ?: ""
                val changeAmount = item.textOrNull("compareToPreviousClosePrice") ?: ""
                val fluctuationsRatio = item.textOrNull("fluctuationsRatio") ?: ""

                val direction = item.path("compareToPreviousPrice").textOrNull("name") ?: ""
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

                MarketIndexRes(
                    type = type.name,
                    name = type.displayName,
                    price = price,
                    changeAmount = changeAmountWithSign,
                    changeRate = changeRateWithSign,
                    delayStatus = "실시간",
                    updatedAt = now,
                )
            }
        } catch (e: MarketIndexApiException) {
            throw e
        } catch (e: MarketIndexResponseException) {
            throw e
        } catch (e: Exception) {
            log.error { "fetchFromHomeMajorsApi Error: ${e.message}" }

            throw RuntimeException(e.message)
        }
    }

    private fun fetchFromPollingApi(now: LocalDateTime): List<MarketIndexRes> {
        val monos = POLLING_PATHS.map { (type, path) ->
            val url = "$naverPcUrl$path"
            webClient.get()
                .uri(url)
                .retrieve()
                .onStatus({ it.isError }, { Mono.error(MarketIndexApiException()) })
                .bodyToMono<String>()
                .map { body -> parsePollingIndex(type, body, now) }
                .onErrorResume { e ->
                    log.error { "[${type.displayName}] polling fetch Error: ${e.message}" }
                    Mono.empty()
                }
        }

        return Mono.zip(monos) { results ->
            results.filterIsInstance<MarketIndexRes>()
        }.block() ?: emptyList()
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
