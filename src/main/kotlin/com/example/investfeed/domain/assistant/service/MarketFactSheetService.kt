package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.circuitbreaker.service.CircuitBreakerService
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.domain.index.dto.res.IndexInfo
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.service.SectService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.chart.client.SectChartClient
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import com.example.investfeed.upbit.candle.client.CandleClient
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class MarketFactSheetService(
    private val indexService: IndexService,
    private val sectService: SectService,
    private val sectClient: SectClient,
    private val sectChartClient: SectChartClient,
    private val marketIndexService: MarketIndexService,
    private val candleClient: CandleClient,
    private val cryptoService: CryptoService,
    private val usTreasuryService: UsTreasuryService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val holidayService: HolidayService,
    private val stockPickRepository: StockPickRepository,
    private val circuitBreakerService: CircuitBreakerService,
    private val krTreasuryFallbackService: KrTreasuryFallbackService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        const val KOSPI = "001"
        const val KOSPI200 = "201"
        const val KOSDAQ = "101"
        const val KOSDAQ150 = "150"
        private val SECTOR_EXCLUDE = listOf("종합", "대형주", "중형주", "소형주", "200", "KRX", "KOSTAR", "변동성", "코스피", "배당")
        const val STREAK_LOOKBACK = 10
        private val CB_INDEX_NAME = mapOf("KOSPI" to "코스피", "KOSDAQ" to "코스닥")
        private val KR_TREASURY_TYPES = listOf(
            MarketIndexType.KR_TREASURY_2Y, MarketIndexType.KR_TREASURY_3Y, MarketIndexType.KR_TREASURY_5Y,
            MarketIndexType.KR_TREASURY_10Y, MarketIndexType.KR_TREASURY_20Y, MarketIndexType.KR_TREASURY_30Y,
        )
    }

    fun collectKrClose(tradeDate: LocalDate, asOf: LocalDateTime): KrCloseFactSheet {
        val kospiInfo = fetchOrNull("코스피 지수") { indexService.getIndexInfo(KOSPI) }
        val kosdaqInfo = fetchOrNull("코스닥 지수") { indexService.getIndexInfo(KOSDAQ) }
        return KrCloseFactSheet(
            tradeDate = tradeDate,
            asOf = asOf,
            kospi = kospiInfo?.let { toIndexFact("코스피", it) },
            kosdaq = kosdaqInfo?.let { toIndexFact("코스닥", it) },
            kospi200 = fetchOrNull("코스피200 일봉") { dailyIndexFact("코스피200", KOSPI200, tradeDate) },
            kosdaq150 = fetchOrNull("코스닥150 일봉") { dailyIndexFact("코스닥150", KOSDAQ150, tradeDate) },
            kospiRate5d = fetchOrNull("코스피 5일 등락") { calcRateOverDays(KOSPI, tradeDate, 5) },
            kosdaqRate5d = fetchOrNull("코스닥 5일 등락") { calcRateOverDays(KOSDAQ, tradeDate, 5) },
            flow = if (kospiInfo == null && kosdaqInfo == null) null else FlowFact(
                kospi = fetchOrNull("코스피 수급") { kospiInfo?.let { marketFlow(it, KOSPI) } },
                kosdaq = fetchOrNull("코스닥 수급") { kosdaqInfo?.let { marketFlow(it, null) } },
            ),
            sectors = fetchOrNull("업종") { fetchSectors() },
            usdKrw = fetchOrNull("환율") { fetchQuote(MarketIndexType.USD_KRW) },
            circuitBreakers = circuitBreakerFacts(tradeDate),
            krTreasury = fetchOrNull("한국 국채") { fetchKrTreasury(tradeDate) },
        )
    }

    fun collectKrPre(tradeDate: LocalDate, asOf: LocalDateTime): KrPreFactSheet {
        return KrPreFactSheet(
            tradeDate = tradeDate,
            asOf = asOf,
            krHoliday = holidayService.isHoliday(asOf.toLocalDate()),
            krPrev = fetchOrNull("어제 국내장") { fetchPrevIndexes(asOf) },
            recommend = fetchOrNull("오늘 추천") { fetchRecommendations() },
            macro = fetchOrNull("매크로") { fetchMacro() },
        )
    }

    private fun fetchPrevIndexes(asOf: LocalDateTime): KrPrevFact {
        val prevDate = holidayService.lastTradingDay(asOf.toLocalDate().minusDays(1))
        return KrPrevFact(
            tradeDate = prevDate,
            kospi = fetchOrNull("코스피 일봉") { dailyIndexFact("코스피", KOSPI, prevDate) },
            kospi200 = fetchOrNull("코스피200 일봉") { dailyIndexFact("코스피200", KOSPI200, prevDate) },
            kosdaq = fetchOrNull("코스닥 일봉") { dailyIndexFact("코스닥", KOSDAQ, prevDate) },
            kosdaq150 = fetchOrNull("코스닥150 일봉") { dailyIndexFact("코스닥150", KOSDAQ150, prevDate) },
        )
    }

    private fun dailyIndexFact(name: String, indsCd: String, date: LocalDate): IndexFact? = runBlocking {
        val rows = sectChartClient.sectChartDayList(SectChartDayListReq(inds_cd = indsCd, base_dt = date.format(YYYYMMDD))).inds_dt_pole_qry.orEmpty()
        val key = date.format(YYYYMMDD)
        val idx = rows.indexOfFirst { it.dt == key }
        if (idx < 0) return@runBlocking null
        fun price(v: String?) = TemplateFormat.parse(v)?.let { abs(it) / 100 }
        val row = rows[idx]
        val close = price(row.cur_prc) ?: return@runBlocking null
        val prevClose = rows.getOrNull(idx + 1)?.let { price(it.cur_prc) }
        IndexFact(
            name = name, close = close,
            changeRate = prevClose?.takeIf { it > 0 }?.let { (close / it - 1) * 100 },
            changeAmount = prevClose?.let { close - it },
            high = price(row.high_pric), low = price(row.low_pric),
            tradeAmount = TemplateFormat.parseLong(row.trde_prica)?.let { abs(it) },
        )
    }

    private fun fetchRecommendations(): List<RecommendFact> {
        val order = listOf("STRONG_BUY", "BUY", "HOLD", "SELL", "STRONG_SELL")
        val picks = stockPickRepository.findAllByOrderByStkCdAsc()
        return picks.groupBy { it.type }.entries
            .sortedBy { order.indexOf(it.key).let { i -> if (i < 0) order.size else i } }
            .map { (grade, rows) -> RecommendFact(grade, rows.map { it.stkNm }.sorted()) }
    }

    fun collectKrNow(asOf: LocalDateTime, kospi: Boolean = true, kosdaq: Boolean = true): KrCloseFactSheet {
        val kospiInfo = if (kospi) fetchOrNull("코스피 지수") { indexService.getIndexInfo(KOSPI) } else null
        val kosdaqInfo = if (kosdaq) fetchOrNull("코스닥 지수") { indexService.getIndexInfo(KOSDAQ) } else null
        return KrCloseFactSheet(
            tradeDate = asOf.toLocalDate(),
            asOf = asOf,
            kospi = kospiInfo?.let { toIndexFact("코스피", it) },
            kosdaq = kosdaqInfo?.let { toIndexFact("코스닥", it) },
            kospi200 = if (kospi) fetchOrNull("코스피200 현재가") { sectPriceNowFact("코스피200", KOSPI200) } else null,
            kosdaq150 = if (kosdaq) fetchOrNull("코스닥150 현재가") { sectPriceNowFact("코스닥150", KOSDAQ150) } else null,
            flow = if (kospiInfo == null && kosdaqInfo == null) null else FlowFact(
                kospi = fetchOrNull("코스피 수급") { kospiInfo?.let { marketFlow(it, null) } },
                kosdaq = fetchOrNull("코스닥 수급") { kosdaqInfo?.let { marketFlow(it, null) } },
            ),
            sectors = null,
            usdKrw = null,
            circuitBreakers = circuitBreakerFacts(holidayService.lastTradingDay(asOf.toLocalDate())).filter { (kospi && it.name == "코스피") || (kosdaq && it.name == "코스닥") },
        )
    }

    private fun circuitBreakerFacts(date: LocalDate): List<CircuitBreakerFact> =
        fetchOrNull("서킷브레이커") {
            circuitBreakerService.findByDate(date).map {
                CircuitBreakerFact(CB_INDEX_NAME[it.indexCode] ?: it.indexCode, it.stage, it.triggerRate, it.startedAt, it.endedAt)
            }
        }.orEmpty()

    fun collectUsNow(asOf: LocalDateTime): UsCloseFactSheet =
        collectUsClose(usMarketCalendarService.lastClosedUsTradingDay(asOf), asOf)

    fun collectUsClose(usTradeDate: LocalDate, asOf: LocalDateTime): UsCloseFactSheet {
        val indexes = fetchOrNull("미국 지수") {
            listOf(
                MarketIndexType.NASDAQ to "나스닥", MarketIndexType.SP500 to "S&P500", MarketIndexType.DOW to "다우",
                MarketIndexType.PHILADELPHIA_SEMICONDUCTOR to "필라델피아 반도체", MarketIndexType.VIX to "VIX",
            ).mapNotNull { (type, name) ->
                fetchQuote(type)?.let { q -> IndexFact(name = name, close = q.price, changeRate = q.changeRate, changeAmount = q.changeAmount, delayStatus = q.delayStatus) }
            }.takeIf { it.isNotEmpty() }
        }
        return UsCloseFactSheet(
            tradeDate = usTradeDate,
            asOf = asOf,
            earlyClose = usMarketCalendarService.isEarlyClose(usTradeDate),
            indexes = indexes,
            treasury = fetchOrNull("미국 국채") { fetchTreasury(usTradeDate) },
            usdKrw = fetchOrNull("환율") { fetchQuote(MarketIndexType.USD_KRW) },
        )
    }

    fun collectKrIndexAlert(indexCode: String): IndexAlertCardFact {
        val (primaryName, primaryCd, secondaryName, secondaryCd) =
            if (indexCode == "KOSDAQ") listOf("코스닥", KOSDAQ, "코스닥150", KOSDAQ150) else listOf("코스피", KOSPI, "코스피200", KOSPI200)
        val info = fetchOrNull("$primaryName 지수") { indexService.getIndexInfo(primaryCd) }
        return IndexAlertCardFact(
            primary = info?.let { toIndexFact(primaryName, it) },
            secondary = fetchOrNull("$secondaryName 현재가") { sectPriceNowFact(secondaryName, secondaryCd) },
            flow = info?.let { fetchOrNull("$primaryName 수급") { marketFlow(it, null) } },
        )
    }

    fun collectUsIndexAlert(): IndexAlertCardFact = IndexAlertCardFact(
        primary = fetchOrNull("나스닥") { fetchQuote(MarketIndexType.NASDAQ)?.let { q -> IndexFact("나스닥", q.price, q.changeRate, q.changeAmount, delayStatus = q.delayStatus) } },
        secondary = fetchOrNull("S&P500") { fetchQuote(MarketIndexType.SP500)?.let { q -> IndexFact("S&P500", q.price, q.changeRate, q.changeAmount, delayStatus = q.delayStatus) } },
        flow = null,
    )

    private fun sectPriceNowFact(name: String, indsCd: String): IndexFact? = runBlocking {
        val res = sectClient.sectPriceNow(KiwoomSectPriceNowReq(mrkt_tp = "0", inds_cd = indsCd))
        val close = TemplateFormat.parse(res.cur_prc)?.let { abs(it) } ?: return@runBlocking null
        IndexFact(
            name = name, close = close,
            changeRate = TemplateFormat.parse(res.flu_rt), changeAmount = TemplateFormat.parse(res.pred_pre),
            high = TemplateFormat.parse(res.high_pric)?.let { abs(it) }, low = TemplateFormat.parse(res.low_pric)?.let { abs(it) },
        )
    }

    private fun toIndexFact(name: String, info: IndexInfo): IndexFact? {
        val close = TemplateFormat.parse(info.curPrc)?.let { abs(it) } ?: return null
        return IndexFact(
            name = name,
            close = close,
            changeRate = TemplateFormat.parse(info.fluRt),
            changeAmount = TemplateFormat.parse(info.predPre),
            high = TemplateFormat.parse(info.highPric)?.let { abs(it) },
            low = TemplateFormat.parse(info.lowPric)?.let { abs(it) },
            tradeAmount = TemplateFormat.parseLong(info.trdePrica)?.let { abs(it) },
            high250 = TemplateFormat.parse(info._250hgst)?.let { abs(it) },
            low250 = TemplateFormat.parse(info._250lwst)?.let { abs(it) },
        )
    }

    private fun calcRateOverDays(indsCd: String, date: LocalDate, days: Int): Double? = runBlocking {
        val rows = sectChartClient.sectChartDayList(SectChartDayListReq(inds_cd = indsCd, base_dt = date.format(YYYYMMDD))).inds_dt_pole_qry.orEmpty()
        val idx = rows.indexOfFirst { it.dt == date.format(YYYYMMDD) }
        if (idx < 0) return@runBlocking null
        val close = TemplateFormat.parse(rows[idx].cur_prc)?.let { abs(it) } ?: return@runBlocking null
        val ref = rows.getOrNull(idx + days)?.let { TemplateFormat.parse(it.cur_prc) }?.let { abs(it) }?.takeIf { it > 0 } ?: return@runBlocking null
        (close / ref - 1) * 100
    }

    private fun marketFlow(info: IndexInfo, streakIndsCd: String?): MarketFlowFact {
        val foreign = TemplateFormat.parseLong(info.frgnrNetprps)
        val history = streakIndsCd?.let { cd ->
            runCatching { indexService.getIndexInvestorDailyList(cd) }.onFailure { log.error(it) { "지수 투자자 일별 조회 실패 ($cd)" } }.getOrDefault(emptyList())
        }.orEmpty()
        return MarketFlowFact(
            foreign = foreign,
            institution = TemplateFormat.parseLong(info.orgnNetprps),
            individual = TemplateFormat.parseLong(info.indNetprps),
            foreignStreak = if (streakIndsCd == null) 0 else countStreakDays(foreign, history.map { TemplateFormat.parseLong(it.frgnrNetprps) }),
        )
    }

    private fun countStreakDays(today: Long?, history: List<Long?>): Int {
        if (today == null || today == 0L) return 0
        val positive = today > 0
        return 1 + history.take(STREAK_LOOKBACK)
            .takeWhile { it != null && it != 0L && (it > 0) == positive }
            .count()
    }

    private fun fetchSectors(): List<SectorFact>? {
        val items = sectService.listSects(SectListReq(indsCd = KOSPI)).sectList.orEmpty()
        return items.mapNotNull { s ->
            val name = s.stkNm ?: return@mapNotNull null
            if (SECTOR_EXCLUDE.any { name.contains(it) }) return@mapNotNull null
            val rate = TemplateFormat.parse(s.fluRt) ?: return@mapNotNull null
            SectorFact(name = name, changeRate = rate, tradeAmount = TemplateFormat.parseLong(s.trdePrica))
        }.takeIf { it.isNotEmpty() }
    }

    private fun fetchKrTreasury(tradeDate: LocalDate): KrTreasuryFact? =
        KR_TREASURY_TYPES.mapNotNull { type ->
            val name = type.displayName.removePrefix("한국 국채 ")
            // 옛 값이어도 버리지 않고 "몇 시 기준"으로 표기. Redis 에 값 자체가 없을 때만 ECOS 종가로 대체
            val res = marketIndexService.getMarketIndex(type)
            val value = res?.let { TemplateFormat.parse(it.price) }
            if (value != null) return@mapNotNull YieldFact(name, value, TemplateFormat.parse(res.changeAmount)?.let { it * 100 }, staleAt = marketIndexService.staleAt(res))
            log.warn { "[${type.displayName}] 네이버 시세 결측 → ECOS 대체 시도" }
            krTreasuryFallbackService.latestClose(type, tradeDate)?.let { YieldFact(name, it.value, it.changeBp, fallbackDate = it.tradeDate) }
        }.takeIf { it.isNotEmpty() }?.let { KrTreasuryFact(it) }

    private fun fetchQuote(type: MarketIndexType): QuoteFact? {
        val res = marketIndexService.getMarketIndex(type) ?: return null
        val price = TemplateFormat.parse(res.price) ?: return null
        // 네이버 수집이 실패하면 Redis(TTL 1일)에 옛 값이 남는다 → 값은 쓰되 마지막 갱신 시각을 함께 넘겨 "몇 시 기준"으로 표기
        return QuoteFact(
            price = price,
            changeRate = TemplateFormat.parse(res.changeRate),
            changeAmount = TemplateFormat.parse(res.changeAmount),
            delayStatus = res.delayStatus,
            staleAt = marketIndexService.staleAt(res),
        )
    }

    private fun fetchMacro(): MacroFact = MacroFact(
        usdKrw = fetchOrNull("환율") { fetchQuote(MarketIndexType.USD_KRW) },
        dollarIndex = fetchOrNull("달러인덱스") { fetchQuote(MarketIndexType.DOLLAR_INDEX) },
        gold = fetchOrNull("금") { fetchQuote(MarketIndexType.GOLD_INTERNATIONAL) },
        wti = fetchOrNull("WTI") { fetchQuote(MarketIndexType.WTI) },
    )

    fun collectCoinDaily(tradeDate: LocalDate, asOf: LocalDateTime): CoinDailyFactSheet = CoinDailyFactSheet(
        tradeDate = tradeDate,
        asOf = asOf,
        btc = fetchOrNull("BTC 일봉") { coinDay("KRW-BTC", "BTC", tradeDate) },
        eth = fetchOrNull("ETH 일봉") { coinDay("KRW-ETH", "ETH", tradeDate) },
        fearGreed = fetchOrNull("공포탐욕") { cryptoService.fearGreedIndex().current.value },
    )

    fun coinDay(market: String, name: String, tradeDate: LocalDate): CoinDayFact? {
        val key = tradeDate.toString() + "T09:00:00"
        val candle = candleClient.getCandlesDays(market, count = 5).firstOrNull { it.candle_date_time_kst == key } ?: return null
        val close = candle.trade_price ?: return null
        return CoinDayFact(
            market = market, name = name, close = close, prevClose = candle.prev_closing_price,
            changeRate = candle.change_rate?.let { it * 100 }, tradeAmount = candle.candle_acc_trade_price,
        )
    }

    private fun fetchTreasury(usTradeDate: LocalDate): TreasuryFact? {
        fun q(type: MarketIndexType) = fetchOrNull("미국 국채 ${type.displayName}") { usTreasuryService.quote(type, usTradeDate) }
        val y2 = q(MarketIndexType.US_TREASURY_2Y)
        val y5 = q(MarketIndexType.US_TREASURY_5Y)
        val y10 = q(MarketIndexType.US_TREASURY_10Y)
        val y30 = q(MarketIndexType.US_TREASURY_30Y)
        val all = listOfNotNull(y2, y5, y10, y30)
        if (all.isEmpty()) return null
        val asOfDate = all.minOf { it.tradeDate }
        val spread = if (y2 != null && y10 != null) Math.round((y10.value - y2.value) * 100).toInt() else null
        return TreasuryFact(
            y2 = y2?.value, y2Bp = y2?.changeBp,
            y10 = y10?.value, y10Bp = y10?.changeBp,
            y5 = y5?.value, y5Bp = y5?.changeBp,
            y30 = y30?.value, y30Bp = y30?.changeBp,
            spreadBp = spread,
            source = all.map { it.source }.let { if (it.contains(UsTreasuryService.SOURCE_FRED)) UsTreasuryService.SOURCE_FRED else UsTreasuryService.SOURCE_NAVER },
            asOfDate = asOfDate,
            stale = asOfDate.isBefore(usTradeDate),
            bondHoliday = usMarketCalendarService.isUsBondHoliday(usTradeDate),
        )
    }

    private fun <T> fetchOrNull(label: String, block: () -> T?): T? =
        runCatching(block).onFailure { log.error(it) { "브리핑 팩트시트 조회 실패: $label" } }.getOrNull()
}
