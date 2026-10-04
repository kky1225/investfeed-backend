package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.service.MarketFactSheetService
import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.assistant.dto.message.Section
import com.example.investfeed.domain.assistant.service.BriefingTemplateRenderer
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.news.service.NewsService
import com.example.investfeed.domain.recommend.repository.StockPickHistoryRepository
import com.example.investfeed.domain.recommend.repository.StockPickRepository
import com.example.investfeed.kiwoom.investor.client.InvestorClient
import com.example.investfeed.kiwoom.investor.dto.req.KiwoomInvestorTradeDayReq
import com.example.investfeed.kiwoom.investor.dto.req.KiwoomInvestorTradeRankListReq
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomDefaultStockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInvestorReq
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.upbit.ticker.client.TickerClient
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class DataToolService(
    private val stockResolver: StockResolver,
    private val periodResolver: PeriodResolver,
    private val marketFactSheetService: MarketFactSheetService,
    private val marketIndexService: MarketIndexService,
    private val investorClient: InvestorClient,
    private val stockClient: StockClient,
    private val usStockClient: UsStockClient,
    private val tickerClient: TickerClient,
    private val economicCalendarService: EconomicCalendarService,
    private val stockPickRepository: StockPickRepository,
    private val stockPickHistoryRepository: StockPickHistoryRepository,
    private val newsService: NewsService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val MAX_STOCKS = 3
        const val MAX_STOCK_FLOW_DAYS = 10
        const val MAX_NEWS = 5
        const val MAX_CALENDAR = 50
        const val MAX_FLOW_STOCKS = 10
        val RANK_PERIODS = listOf(1, 3, 5, 10, 20)
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    enum class Market { KOSPI, KOSDAQ }
    enum class Investor { FOREIGN, INSTITUTION, PENSION, INDIVIDUAL }
    enum class Side { BUY, SELL }

    data class MarketCard(val title: String, val asOf: LocalDateTime, val summary: String, val sections: List<Section>)

    fun marketSummary(market: Market?, date: LocalDate?): MarketCard {
        val tradingDay = periodResolver.lastTradingDay()
        if (date != null && date != tradingDay) throw ToolException("지수는 직전 개장일($tradingDay)만 조회할 수 있습니다")
        val now = LocalDateTime.now()
        val kospi = market != Market.KOSDAQ
        val kosdaq = market != Market.KOSPI
        val sheet = marketFactSheetService.collectKrNow(now, kospi = kospi, kosdaq = kosdaq)
        if (sheet.kospi == null && sheet.kosdaq == null) throw ToolException("국내 지수 조회에 실패했습니다")
        val r = BriefingTemplateRenderer.renderKrMarketNow(sheet, kospi = kospi, kosdaq = kosdaq)
        val title = when (market) { Market.KOSPI -> "코스피"; Market.KOSDAQ -> "코스닥"; null -> "국내 시장" }
        return MarketCard(title, now, r.summary, r.sections)
    }

    /** netAmountEok: 순매수 금액(억원). 키움은 백만원 단위로 주므로 /100 (대시보드와 같은 변환) */
    data class FlowStock(val code: String, val name: String, val netAmountEok: Long, val periodChangeRate: Double?, val streakDays: Int?)
    data class MarketInvestorFlow(val investor: Investor, val market: Market, val days: Int, val side: Side, val minStreakDays: Int?, val stocks: List<FlowStock>)

    fun marketInvestorFlow(investor: Investor, market: Market, days: Int, side: Side, minStreakDays: Int?): MarketInvestorFlow {
        if (days !in RANK_PERIODS) throw ToolException("days 는 ${RANK_PERIODS.joinToString("/")} 중 하나여야 합니다")
        if (minStreakDays != null && investor !in listOf(Investor.FOREIGN, Investor.INSTITUTION)) throw ToolException("연속 순매수 조건은 외국인·기관만 지원합니다")
        val mrktTp = if (market == Market.KOSPI) "001" else "101"
        val stocks = when (investor) {
            Investor.FOREIGN, Investor.INSTITUTION -> {
                val res = runBlocking { investorClient.investorTradeRankList(KiwoomInvestorTradeRankListReq(dt = days.toString(), mrkt_tp = mrktTp, stk_inds_tp = "0", amt_qty_tp = "0", stex_tp = "3")) }
                (res.orgn_frgnr_cont_trde_prst ?: emptyList()).mapNotNull { r ->
                    val amt = TemplateFormat.parseLong(if (investor == Investor.FOREIGN) r.frgnr_nettrde_amt else r.orgn_nettrde_amt) ?: return@mapNotNull null
                    val streak = TemplateFormat.parseLong(if (investor == Investor.FOREIGN) r.frgnr_cont_netprps_dys else r.orgn_cont_netprps_dys)?.toInt()
                    FlowStock(r.stk_cd?.substringBefore("_") ?: return@mapNotNull null, r.stk_nm ?: "", amt / 100, TemplateFormat.parse(r.prid_stkpc_flu_rt), streak)
                }
            }
            Investor.PENSION, Investor.INDIVIDUAL -> {
                val end = periodResolver.lastTradingDay()
                val start = periodResolver.tradingDaysBack(days, end)
                val res = runBlocking {
                    investorClient.investorTradeDay(KiwoomInvestorTradeDayReq(
                        strt_dt = start.format(YYYYMMDD), end_dt = end.format(YYYYMMDD), trde_tp = if (side == Side.BUY) "2" else "1",
                        mrkt_tp = mrktTp, invsr_tp = if (investor == Investor.PENSION) "6000" else "8000", stex_tp = "3",
                    ))
                }
                (res?.invsr_daly_trde_stk ?: emptyList()).mapNotNull { r ->
                    val amt = TemplateFormat.parseLong(r.netslmt_amt) ?: return@mapNotNull null
                    FlowStock(r.stk_cd?.substringBefore("_") ?: return@mapNotNull null, r.stk_nm ?: "", (if (side == Side.BUY) abs(amt) else -abs(amt)) / 100, null, null)
                }
            }
        }
        val filtered = stocks
            .filter { if (side == Side.BUY) it.netAmountEok > 0 else it.netAmountEok < 0 }
            .filter { minStreakDays == null || (it.streakDays ?: 0) >= minStreakDays }
            .sortedByDescending { abs(it.netAmountEok) }
            .take(MAX_FLOW_STOCKS)
        return MarketInvestorFlow(investor, market, days, side, minStreakDays, filtered)
    }

    // ── get_stock_quote ──

    data class StockQuote(
        val code: String, val name: String, val market: StockMarket, val link: String, val currency: String,
        val price: Double, val changeRate: Double?, val changeAmount: Double?, val volume: Double?,
        val high: Double?, val low: Double?, val high52w: Double?, val low52w: Double?,
    )

    fun stockQuotes(queries: List<String>, memberId: Long, marketHint: StockMarket?): List<StockQuote> {
        if (queries.isEmpty()) throw ToolException("종목을 입력해 주세요")
        if (queries.size > MAX_STOCKS) throw ToolException("종목은 최대 ${MAX_STOCKS}개까지 비교할 수 있습니다")
        return queries.map { q -> quote(stockResolver.resolve(q, memberId, marketHint)) }
    }

    private fun quote(s: ResolvedStock): StockQuote = when (s.market) {
        StockMarket.KR -> runBlocking {
            val info = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = s.code)).atn_stk_infr?.firstOrNull()
                ?: throw ToolException("${s.name} 시세 조회에 실패했습니다")
            val def = runCatching { stockClient.stockDefaultInfo(KiwoomDefaultStockInfoReq(stk_cd = s.code)) }
                .onFailure { log.error(it) { "종목 기본정보 조회 실패 ${s.code}" } }.getOrNull()
            fun p(v: String?) = TemplateFormat.parse(v)?.let { abs(it) }
            StockQuote(
                s.code, s.name, s.market, s.link, "KRW",
                price = p(info.cur_prc) ?: throw ToolException("${s.name} 현재가가 없습니다"),
                changeRate = TemplateFormat.parse(info.flu_rt), changeAmount = TemplateFormat.parse(info.pred_pre), volume = p(info.trde_qty),
                high = p(info.high_pric), low = p(info.low_pric), high52w = p(def?._250hgst), low52w = p(def?._250lwst),
            )
        }
        StockMarket.US -> {
            val r = usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = s.stexTp ?: "ND", stk_cd = s.code))
            fun p(v: String?) = TemplateFormat.parse(v)?.let { abs(it) }
            StockQuote(
                s.code, s.name, s.market, s.link, "USD",
                price = p(r.cur_prc) ?: throw ToolException("${s.name} 현재가가 없습니다"),
                changeRate = TemplateFormat.parse(r.flu_rt), changeAmount = TemplateFormat.parse(r.pred_pre), volume = p(r.acc_trde_qty),
                high = p(r.high_pric), low = p(r.low_pric), high52w = p(r.wk52_hgst_pric), low52w = p(r.wk52_lwst_pric),
            )
        }
        StockMarket.CRYPTO -> {
            val t = tickerClient.getTickers(s.code).firstOrNull() ?: throw ToolException("${s.name} 시세 조회에 실패했습니다")
            val price = t.trade_price ?: throw ToolException("${s.name} 현재가가 없습니다")
            val prev = t.prev_closing_price?.takeIf { it > 0 }
            StockQuote(
                s.code, s.name, s.market, s.link, "KRW",
                price = price, changeRate = prev?.let { (price / it - 1) * 100 }, changeAmount = prev?.let { price - it }, volume = t.acc_trade_volume_24h,
                high = t.high_price, low = t.low_price, high52w = t.highest_52_week_price, low52w = t.lowest_52_week_price,
            )
        }
    }

    // ── get_stock_investor_flow ──

    /** 순매수 금액은 억원 (ka10059 amt_qty_tp=1 은 백만원 → /100). changeRate 는 ka10059 flu_rt 가 100배(+167 = +1.67%)라 /100 */
    data class StockFlowDay(val date: String, val foreignEok: Long?, val institutionEok: Long?, val individualEok: Long?, val close: Double?, val changeRate: Double?)
    data class StockInvestorFlow(
        val code: String, val name: String, val link: String, val days: List<StockFlowDay>,
        val foreignStreak: Int, val institutionStreak: Int,   // 최근일부터 같은 방향 연속일수. 양수=순매수, 음수=순매도
    )

    /** 국내만 (ka10059 금액 기준). 미국·코인은 종목별 수급 데이터가 없다 (Q-10) */
    fun stockInvestorFlows(queries: List<String>, memberId: Long, days: Int): List<StockInvestorFlow> {
        if (queries.isEmpty()) throw ToolException("종목을 입력해 주세요")
        if (queries.size > MAX_STOCKS) throw ToolException("종목은 최대 ${MAX_STOCKS}개까지 비교할 수 있습니다")
        val n = days.coerceIn(1, MAX_STOCK_FLOW_DAYS)
        return queries.map { q ->
            val s = stockResolver.resolve(q, memberId)
            if (s.market != StockMarket.KR) throw ToolException("종목별 수급은 국내 주식만 조회할 수 있습니다: ${s.name}")
            val rows = runBlocking {
                stockClient.stockInvestor(KiwoomStockInvestorReq(dt = LocalDate.now().format(YYYYMMDD), stk_cd = s.assetCode, amt_qty_tp = "1", trde_tp = "0", unit_tp = "1"))
            }.stk_invsr_orgn?.take(n) ?: emptyList()
            val list = rows.map { r ->
                fun eok(v: String?) = TemplateFormat.parseLong(v)?.let { it / 100 }
                StockFlowDay(r.dt ?: "", eok(r.frgnr_invsr), eok(r.orgn), eok(r.ind_invsr),
                    TemplateFormat.parse(r.cur_prc)?.let { abs(it) }, TemplateFormat.parse(r.flu_rt)?.let { it / 100 })
            }
            StockInvestorFlow(s.code, s.name, s.link, list, streak(list.map { it.foreignEok }), streak(list.map { it.institutionEok }))
        }
    }

    /** 최근일부터 부호가 같은 날 수. 첫 값이 0·null 이면 0 */
    private fun streak(values: List<Long?>): Int {
        val first = values.firstOrNull()?.takeIf { it != 0L } ?: return 0
        val sign = if (first > 0) 1 else -1
        val count = values.takeWhile { v -> v != null && v != 0L && (if (v > 0) 1 else -1) == sign }.size
        return count * sign
    }

    /** 미국 시장: 미국 마감 브리핑과 같은 지수 타일(나스닥·S&P500·다우·필라델피아 반도체·VIX) + 국채 + 환율 */
    fun globalIndexes(): MarketCard {
        val now = LocalDateTime.now()
        val sheet = marketFactSheetService.collectUsNow(now)
        if (sheet.indexes == null) throw ToolException("미국 지수 조회에 실패했습니다")
        val r = BriefingTemplateRenderer.renderUsMarketNow(sheet)
        return MarketCard("미국 시장", now, r.summary, r.sections)
    }

    data class CalendarItem(val date: String, val name: String, val country: String, val type: String, val value: String?)

    enum class CalendarType(val eventTypes: Set<String>) {
        INDICATOR(setOf("INDICATOR", "GDP_RELEASE", "CPI_RELEASE")),
        HOLIDAY(setOf("HOLIDAY")),
        MEETING(setOf("RATE_DECISION", "US_RATE_DECISION")),
    }

    enum class CalendarCountry { KR, US }

    fun calendar(from: LocalDate, to: LocalDate, keyword: String?, type: CalendarType?, country: CalendarCountry?): List<CalendarItem> {
        if (to.isBefore(from)) throw ToolException("종료일이 시작일보다 앞섭니다")
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) > 92) throw ToolException("일정은 최대 3개월 범위로 조회할 수 있습니다")
        var ym = YearMonth.from(from)
        val end = YearMonth.from(to)
        val out = mutableListOf<CalendarItem>()
        while (!ym.isAfter(end)) {
            val events = runCatching { economicCalendarService.listEvents(ym.year, ym.monthValue).events }
                .onFailure { log.error(it) { "캘린더 조회 실패 $ym" } }.getOrDefault(emptyList())
            out += events
                .filter { runCatching { LocalDate.parse(it.date) }.getOrNull()?.let { d -> !d.isBefore(from) && !d.isAfter(to) } ?: false }
                .filter { keyword.isNullOrBlank() || it.name.contains(keyword, ignoreCase = true) }
                .filter { type == null || it.type in type.eventTypes }
                .filter { country == null || it.country == country.name }
                .map { CalendarItem(it.date, it.name, it.country, it.type, it.value) }
            ym = ym.plusMonths(1)
        }
        return out.sortedBy { it.date }.take(MAX_CALENDAR)
    }

    data class RecommendItem(val code: String, val name: String, val grade: String)
    data class RecommendList(val label: String, val pickDate: LocalDateTime?, val items: List<RecommendItem>)

    /** 당일 추천 (C-12: 모의투자 아님). 등급명만, 사유 없음 */
    fun recommendList(grade: String?): RecommendList {
        val picks = stockPickRepository.findAllByOrderByStkCdAsc()
            .filter { grade == null || it.type.equals(grade, ignoreCase = true) }
            .map { RecommendItem(it.stkCd.substringBefore("_"), it.stkNm, it.type) }
        return RecommendList("추천 종목", stockPickHistoryRepository.findMaxPickDate(), picks)
    }

    data class NewsRow(val title: String, val link: String, val pubDate: String)

    fun searchNews(query: String, n: Int): List<NewsRow> {
        if (query.isBlank()) throw ToolException("검색어를 입력해 주세요")
        return newsService.searchNews(query, 1).items.take(n.coerceIn(1, MAX_NEWS)).map { NewsRow(it.title, it.link, it.pubDate) }
    }
}
