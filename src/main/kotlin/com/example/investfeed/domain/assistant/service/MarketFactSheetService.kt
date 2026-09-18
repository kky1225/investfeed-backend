package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
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
    private val sectChartClient: SectChartClient,
    private val marketIndexService: MarketIndexService,
    private val candleClient: CandleClient,
    private val cryptoService: CryptoService,
    private val usIndexDailyService: UsIndexDailyService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val holidayService: HolidayService,
    private val stockPickRepository: StockPickRepository,
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

    private fun fetchQuote(type: MarketIndexType): QuoteFact? {
        val res = marketIndexService.getMarketIndex(type) ?: return null
        val price = TemplateFormat.parse(res.price) ?: return null
        return QuoteFact(
            price = price,
            changeRate = TemplateFormat.parse(res.changeRate),
            changeAmount = TemplateFormat.parse(res.changeAmount),
            delayStatus = res.delayStatus,
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
        val y2 = usIndexDailyService.findByDate("US2Y", usTradeDate) ?: usIndexDailyService.latestOnOrBefore("US2Y", usTradeDate)
        val y10 = usIndexDailyService.findByDate("US10Y", usTradeDate) ?: usIndexDailyService.latestOnOrBefore("US10Y", usTradeDate)
        if (y2 == null && y10 == null) return null
        val asOfDate = listOfNotNull(y2?.tradeDate, y10?.tradeDate).min()
        val spread = if (y2 != null && y10 != null) Math.round((y10.value - y2.value) * 100).toInt() else null
        return TreasuryFact(
            y2 = y2?.value, y2Bp = y2?.change,
            y10 = y10?.value, y10Bp = y10?.change,
            spreadBp = spread,
            source = listOfNotNull(y2?.source, y10?.source).let { if (it.contains(UsIndexDailyService.SOURCE_FRED)) UsIndexDailyService.SOURCE_FRED else UsIndexDailyService.SOURCE_NAVER },
            asOfDate = asOfDate,
            stale = asOfDate.isBefore(usTradeDate),
        )
    }

    private fun <T> fetchOrNull(label: String, block: () -> T?): T? =
        runCatching(block).onFailure { log.error(it) { "브리핑 팩트시트 조회 실패: $label" } }.getOrNull()
}
