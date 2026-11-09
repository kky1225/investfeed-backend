package com.example.investfeed.domain.marketindex.crawler

import com.example.investfeed.common.util.logHttpError
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.exception.MarketIndexResponseException
import com.example.investfeed.domain.monitoring.enum.ApiProvider
import com.example.investfeed.domain.monitoring.service.ApiCallCounterService
import com.example.investfeed.domain.monitoring.service.ApiCallMetricsFilter
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
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class NaverMarketIndexCrawler(
    @param:Value("\${naver-stock.api-url}")
    private val naverApiUrl: String,
    @param:Value("\${naver-stock.mobile-url}")
    private val naverMobileUrl: String,
    @param:Value("\${naver-stock.pc-url}")
    private val naverPcUrl: String,
    @param:Value("\${naver-stock.polling-url:https://polling.finance.naver.com}")
    private val naverPollingUrl: String,
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
        .filter(ApiCallMetricsFilter(ApiProvider.NAVER_CRAWL, apiCallCounterService))
        .build()

    companion object {
        private val WORLD_INDEX_PATHS = mapOf(
            MarketIndexType.NASDAQ to "/index/.IXIC/basic",
            MarketIndexType.SP500 to "/index/.INX/basic",
            MarketIndexType.VIX to "/index/.VIX/basic",
            MarketIndexType.PHILADELPHIA_SEMICONDUCTOR to "/index/.SOX/basic",
            MarketIndexType.DOW to "/index/.DJI/basic",
        )

        private val DOMESTIC_INDEX_PATHS = mapOf(
            MarketIndexType.KOSPI to "/api/index/KOSPI/basic",
            MarketIndexType.KOSDAQ to "/api/index/KOSDAQ/basic",
        )

        private val EXCHANGE_PATHS = mapOf(
            MarketIndexType.USD_KRW to "/marketindex/exchange/FX_USDKRW",
        )

        private const val EXCHANGE_PAGE_SIZE = 60
        private const val EXCHANGE_MAX_PAGES = 5

        private val BOND_PATHS = mapOf(
            MarketIndexType.US_TREASURY_2Y to "/marketindex/bond/US2YT=RR",
            MarketIndexType.US_TREASURY_5Y to "/marketindex/bond/US5YT=RR",
            MarketIndexType.US_TREASURY_10Y to "/marketindex/bond/US10YT=RR",
            MarketIndexType.US_TREASURY_30Y to "/marketindex/bond/US30YT=RR",
            MarketIndexType.KR_TREASURY_2Y to "/marketindex/bond/KR2YT=RR",
            MarketIndexType.KR_TREASURY_3Y to "/marketindex/bond/KR3YT=RR",
            MarketIndexType.KR_TREASURY_5Y to "/marketindex/bond/KR5YT=RR",
            MarketIndexType.KR_TREASURY_10Y to "/marketindex/bond/KR10YT=RR",
            MarketIndexType.KR_TREASURY_20Y to "/marketindex/bond/KR20YT=RR",
            MarketIndexType.KR_TREASURY_30Y to "/marketindex/bond/KR30YT=RR",
        )

        private val POLLING_PATHS = mapOf(
            MarketIndexType.WTI to "/api/polling/marketindex/energy/CLcv1",
            MarketIndexType.GOLD_INTERNATIONAL to "/api/polling/marketindex/metals/GCcv1",
            MarketIndexType.DOLLAR_INDEX to "/api/polling/marketindex/exchange/.DXY",
        )

        // 장중 시가·고가·저가 실시간 (polling API). basic 에는 고저가가 없고 /index/{code}/price 는 마감 후에만 당일 행이 생긴다 (2026-09-21 확인).
        // 비서 장중 알림이 미국 세션 동안만 조회
        private val OHLC_PATHS = mapOf(
            MarketIndexType.NASDAQ to "/api/realtime/worldstock/index/.IXIC",
            MarketIndexType.SP500 to "/api/realtime/worldstock/index/.INX",
        )
    }

    /** 장중 당일 시가·고가·저가·현재가. prevClose 는 현재가 − 전일 대비(부호 반영) */
    data class IndexOhlc(
        val tradeDate: LocalDate,   // 미국 현지 거래일 (localTradedAt 의 날짜부)
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val prevClose: Double?,
        val marketStatus: String?,  // OPEN / CLOSE
    )

    fun fetchIndexOhlc(type: MarketIndexType): IndexOhlc? = runBlocking {
        val path = OHLC_PATHS[type] ?: return@runBlocking null
        fetchOne(type, "$naverPollingUrl$path") { indexType, body -> parseOhlc(indexType, body) }
    }

    private fun parseOhlc(type: MarketIndexType, body: String): IndexOhlc {
        val data = objectMapper.readTree(body)?.get("datas")?.firstOrNull() ?: throw MarketIndexResponseException()
        fun raw(field: String): Double? = data.textOrNull(field)?.replace(",", "")?.toDoubleOrNull()
        val tradedAt = data.textOrNull("localTradedAt")?.takeIf { it.length >= 10 } ?: throw MarketIndexResponseException()
        val close = raw("closePriceRaw") ?: throw MarketIndexResponseException()
        // 네이버 값은 하락일에 이미 음수(-345.35)로 온다. 크기만 취해 방향으로 부호를 붙여야 이중 반전이 없다
        // (부호를 그대로 뒤집어 하락일 전일 종가가 낮게 잡혀 +3% 오알림 발생, 2026-10-09)
        val change = raw("compareToPreviousClosePriceRaw")?.let { amount ->
            when (data.path("compareToPreviousPrice").textOrNull("name")) {
                "FALLING" -> -kotlin.math.abs(amount); "RISING" -> kotlin.math.abs(amount); else -> 0.0
            }
        }
        val openRaw = raw("openPriceRaw")
        val highRaw = raw("highPriceRaw")
        val lowRaw = raw("lowPriceRaw")
        if (openRaw == null || highRaw == null || lowRaw == null) {
            log.warn { "[${type.displayName}] 시·고·저가 누락 — 현재가로 대체 (localTradedAt=$tradedAt, close=$close)" }
        }

        return IndexOhlc(
            tradeDate = LocalDate.parse(tradedAt.substring(0, 10)),
            open = openRaw ?: close,
            high = highRaw ?: close,
            low = lowRaw ?: close,
            close = close,
            prevClose = change?.let { close - it }?.takeIf { it > 0 },
            marketStatus = data.textOrNull("marketStatus"),
        )
    }

    /** 채권 일별 확정 종가 1행. tradeDate 는 현지 거래일 (localTradedAt 의 날짜부) */
    data class BondDailyClose(val tradeDate: LocalDate, val close: Double, val changeBp: Double?)

    /**
     * 채권 일별 확정 종가 (네이버 `/marketindex/bond/{코드}/prices`, 최신순). 그날 장이 끝난 뒤 행이 생기고(미국 17:05 ET),
     * 채권시장 휴장일에는 행이 없다 (2026-10-12 콜럼버스 데이 확인). 실시간 시세가 아니라 종가 저장·보정용
     */
    fun fetchBondDailyCloses(type: MarketIndexType, pageSize: Int = 10): List<BondDailyClose> = runBlocking {
        val path = BOND_PATHS[type] ?: return@runBlocking emptyList()
        fetchOne(type, "$naverApiUrl$path/prices?page=1&pageSize=$pageSize") { _, body -> parseBondDailyCloses(body) }.orEmpty()
    }

    private fun parseBondDailyCloses(body: String): List<BondDailyClose> {
        val rows = objectMapper.readTree(body)?.takeIf { it.isArray } ?: throw MarketIndexResponseException()
        return rows.mapNotNull { row ->
            val tradedAt = row.textOrNull("localTradedAt")?.takeIf { it.length >= 10 } ?: return@mapNotNull null
            val close = row.textOrNull("closePrice")?.replace(",", "")?.toDoubleOrNull() ?: return@mapNotNull null
            // 하락일 변동값은 이미 음수로 오기도 해서 크기만 취해 방향으로 부호를 붙인다 (parseOhlc 와 같은 이유)
            val change = row.textOrNull("fluctuations")?.replace(",", "")?.toDoubleOrNull()?.let { amount ->
                when (row.path("fluctuationsType").textOrNull("name")) {
                    "FALLING" -> -kotlin.math.abs(amount); "RISING" -> kotlin.math.abs(amount); else -> 0.0
                }
            }
            BondDailyClose(LocalDate.parse(tradedAt.substring(0, 10)), close, change?.let { it * 100 })   // %p → bp
        }
    }

    data class ExchangeDailyClose(val tradeDate: LocalDate, val close: Double, val change: Double?, val changeRate: Double?)

    /**
     * 그날 최종 고시 (네이버 `/marketindex/exchange/{코드}/prices`, 최신순). 그날이 나올 때까지 쪽을 넘긴다
     * (최대 [EXCHANGE_MAX_PAGES]쪽). 고시가 없는 날(휴일)이면 null
     */
    fun fetchExchangeCloseOn(type: MarketIndexType, date: LocalDate): ExchangeDailyClose? = runBlocking {
        val path = EXCHANGE_PATHS[type] ?: return@runBlocking null
        for (page in 1..EXCHANGE_MAX_PAGES) {
            val rows = fetchOne(type, "$naverApiUrl$path/prices?page=$page&pageSize=$EXCHANGE_PAGE_SIZE") { _, body -> parseExchangeDailyCloses(body) }
                ?: return@runBlocking null
            rows.firstOrNull { it.tradeDate == date }?.let { return@runBlocking it }
            // 이 쪽의 가장 옛 행이 그날보다 앞서면 그날 고시가 없는 것 (휴일)
            if (rows.isEmpty() || rows.last().tradeDate.isBefore(date)) return@runBlocking null
        }
        null
    }

    private fun parseExchangeDailyCloses(body: String): List<ExchangeDailyClose> {
        val rows = objectMapper.readTree(body)?.takeIf { it.isArray } ?: throw MarketIndexResponseException()
        return rows.mapNotNull { row ->
            val tradedAt = row.textOrNull("localTradedAt")?.takeIf { it.length >= 10 } ?: return@mapNotNull null
            val close = row.textOrNull("closePrice")?.replace(",", "")?.toDoubleOrNull() ?: return@mapNotNull null
            // 하락일 값이 이미 음수로 오기도 해서 크기만 취해 방향으로 부호를 붙인다 (parseOhlc 와 같은 이유)
            fun signed(field: String): Double? = row.textOrNull(field)?.replace(",", "")?.toDoubleOrNull()?.let { amount ->
                when (row.path("fluctuationsType").textOrNull("name")) {
                    "FALLING" -> -kotlin.math.abs(amount); "RISING" -> kotlin.math.abs(amount); else -> 0.0
                }
            }
            ExchangeDailyClose(LocalDate.parse(tradedAt.substring(0, 10)), close, signed("fluctuations"), signed("fluctuationsRatio"))
        }
    }

    fun crawl(): List<MarketIndexRes> = runBlocking { crawlSuspend() }

    private suspend fun crawlSuspend(): List<MarketIndexRes> {
        val now = LocalDateTime.now()

        val worldApis = WORLD_INDEX_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }
        val domesticApis = DOMESTIC_INDEX_PATHS.map { (type, path) -> type to "$naverMobileUrl$path" }

        val results = fetchAll(worldApis + domesticApis) { type, body -> parseJsonIndex(type, body, now) } +
            fetchAll(EXCHANGE_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }) { type, body -> parseExchangeDetail(type, body, now) } +
            fetchAll(POLLING_PATHS.map { (type, path) -> type to "$naverPcUrl$path" }) { type, body -> parsePollingIndex(type, body, now) } +
            fetchAll(BOND_PATHS.map { (type, path) -> type to "$naverApiUrl$path" }) { type, body -> parseBondDetail(type, body, now) }

        // 네이버 장애면 21건이 한꺼번에 실패하므로 연결·HTTP 실패는 한 번 실행에 error 1건으로 묶는다 (해석 실패는 항목마다 이미 error)
        val requestFailed = results.mapNotNull { (type, r) -> (r as? FetchResult.RequestFailed)?.let { type to it.reason } }
        if (requestFailed.isNotEmpty()) {
            log.error {
                "시장지표 ${results.size}개 중 ${requestFailed.size}개 조회 실패: " +
                    "${requestFailed.map { it.first.displayName }}: ${requestFailed.first().second}"
            }
        }

        return results.mapNotNull { (_, r) -> (r as? FetchResult.Success<MarketIndexRes>)?.value }
    }

    /** 각 URL 을 동시에 조회해 파싱한다. 결과는 항목별 성공·실패 그대로 돌려준다 */
    private suspend fun fetchAll(
        targets: List<Pair<MarketIndexType, String>>,
        parse: (MarketIndexType, String) -> MarketIndexRes,
    ): List<Pair<MarketIndexType, FetchResult<MarketIndexRes>>> = coroutineScope {
        targets.map { (type, url) -> async { type to fetch(type, url, parse) } }.awaitAll()
    }

    /** 단건 조회. 실패 1건에 error 1건 (연결·HTTP 실패는 여기서, 해석 실패는 [fetch] 에서) */
    private suspend fun <T> fetchOne(
        type: MarketIndexType,
        url: String,
        parse: (MarketIndexType, String) -> T,
    ): T? = when (val r = fetch(type, url, parse)) {
        is FetchResult.Success -> r.value
        is FetchResult.RequestFailed -> null.also { log.error { "[${type.displayName}] fetch Error: ${r.reason}" } }
        FetchResult.ParseFailed -> null
    }

    private sealed interface FetchResult<out T> {
        data class Success<T>(val value: T) : FetchResult<T>
        /** 연결·HTTP 실패. warn 만 남긴 상태라 호출부가 error 를 남긴다 */
        data class RequestFailed(val reason: String) : FetchResult<Nothing>
        /** 응답 해석 실패. error 를 이미 남겼다 */
        data object ParseFailed : FetchResult<Nothing>
    }

    /**
     * 조회 후 파싱한다. 연결·HTTP 실패는 필터·logHttpError 가 warn 으로만 남기고 사유를 돌려준다
     * (RETRY_PENDING_ATTR 는 재시도가 아니라 "error 는 호출부가 남긴다"는 뜻으로 쓴다).
     */
    private suspend fun <T> fetch(
        type: MarketIndexType,
        url: String,
        parse: (MarketIndexType, String) -> T,
    ): FetchResult<T> {
        val body = try {
            webClient.get()
                .uri(url)
                .attribute(ApiCallMetricsFilter.RETRY_PENDING_ATTR, true)
                .retrieve()
                .onStatus({ it.isError }, { res -> res.logHttpError("네이버 지수", asWarn = true); res.createException() })
                .awaitBody<String>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return FetchResult.RequestFailed("${e.javaClass.simpleName}: ${e.message}")
        }

        return try {
            FetchResult.Success(parse(type, body))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error { "[${type.displayName}] 응답 해석 실패: ${e.javaClass.simpleName}: ${e.message}, body=${body.take(200)}" }
            FetchResult.ParseFailed
        }
    }

    private fun parseExchangeDetail(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val root = objectMapper.readTree(body)
        val data = root.get("exchangeInfo") ?: throw MarketIndexResponseException()
        return parseExchangeNode(type, data, now)
    }

    private fun parseBondDetail(type: MarketIndexType, body: String, now: LocalDateTime): MarketIndexRes {
        val root = objectMapper.readTree(body) ?: throw MarketIndexResponseException()
        if (root.get("closePrice") == null) throw MarketIndexResponseException()
        return parseExchangeNode(type, root, now)
    }

    private fun parseExchangeNode(type: MarketIndexType, data: JsonNode, now: LocalDateTime): MarketIndexRes {

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
