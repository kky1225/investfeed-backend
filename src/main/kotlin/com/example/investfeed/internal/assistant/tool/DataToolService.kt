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
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.service.InvestorService
import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
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
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.kiwoom.chart.client.StockChartClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartDayReq
import com.example.investfeed.kiwoom.us.chart.client.UsStockChartClient
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import com.example.investfeed.upbit.candle.client.CandleClient
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    private val holidayService: HolidayService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val stockChartClient: StockChartClient,
    private val usStockChartClient: UsStockChartClient,
    private val candleClient: CandleClient,
    private val investorService: InvestorService,
    private val usSessionBaseService: UsSessionBaseService,
) {
    private val log = KotlinLogging.logger {}

    /** 도구 인자 범위의 기준. Python registry.py 도구 설명 문구("종목 1~3개" 등)도 이 값에 맞춘다 */
    companion object {
        const val MAX_STOCKS = 3
        const val MAX_STOCK_FLOW_DAYS = 10
        const val MAX_NEWS = 5
        const val MAX_CALENDAR = 50
        const val MAX_FLOW_STOCKS = 100   // 키움 1회 응답 전부. 카드는 처음 10개만 보이고 더보기로 카드 안 스크롤 (2026-10-18)
        val STREAK_DAYS_RANGE = 2..20
        val RANK_PERIODS = listOf(1, 3, 5, 10, 20)
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val KR_OPEN: LocalTime = LocalTime.of(9, 0)
        private val KR_CLOSE: LocalTime = LocalTime.of(15, 30)
        private val KR_FLOW_FINAL: LocalTime = LocalTime.of(20, 0)   // 투자자 수급 확정 시각 (그 전 당일 수급은 잠정)
        private val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
        private val RECOMMEND_TIME: LocalTime = LocalTime.of(22, 0)   // 추천 산출 스케줄러 (SchedulerCron.RECOMMEND)
        private val KR_FLOW_CLOSED: LocalTime = LocalTime.of(20, 1)   // 투자자별 확정치 기준 (InvestorService 와 같음)
    }

    enum class Market { KOSPI, KOSDAQ }
    enum class Investor { FOREIGN, INSTITUTION, PENSION, INDIVIDUAL }
    enum class Side { BUY, SELL }

    data class MarketCard(val title: String, val asOf: LocalDateTime, val summary: String, val sections: List<Section>)

    data class TextReply(val reply: String)

    /**
     * 국내 지수 카드. date 미지정 = 장중이면 오늘, 아니면 직전 거래일. 지정하면 그날 (미래·휴장일·개장 전이면 문장으로 답).
     * 오늘 장중은 실시간 시세, 지난 날짜는 업종 일봉 + 저장된 확정 수급
     */
    fun marketSummary(market: Market?, date: LocalDate?): Any {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val openToday = !holidayService.isHoliday(today) && !now.toLocalTime().isBefore(KR_OPEN)
        val latest = if (openToday) today else holidayService.lastTradingDay(today.minusDays(1))
        val day = date ?: latest
        val md = TemplateFormat.monthDay(day)
        when {
            day.isAfter(today) -> return TextReply("${md}은 아직 장이 열리지 않았습니다.")
            holidayService.isHoliday(day) -> return TextReply("${md}은 휴장일입니다.")
            day == today && !openToday -> return TextReply("오늘 장은 아직 열리지 않았습니다. (09:00 개장)")
        }

        val kospi = market != Market.KOSDAQ
        val kosdaq = market != Market.KOSPI
        val live = day == today
        val sheet = if (live) {
            marketFactSheetService.collectKrNow(now, kospi = kospi, kosdaq = kosdaq)
        } else {
            // 직전 거래일 수급은 다음 날 07:00 에 저장된다 → 그 전이면 실시간 조회값(직전 거래일 확정치)으로 채운다
            marketFactSheetService.collectKrOn(day, now, kospi = kospi, kosdaq = kosdaq).let { s ->
                if (s.flow == null && day == latest) s.copy(flow = marketFactSheetService.collectKrNow(now, kospi = kospi, kosdaq = kosdaq).flow) else s
            }
        }
        if (sheet.kospi == null && sheet.kosdaq == null) throw ToolException("$md 국내 지수 조회에 실패했습니다")
        val closed = !live || !now.toLocalTime().isBefore(KR_CLOSE)
        val provisional = live && now.toLocalTime().isBefore(KR_FLOW_FINAL)
        val r = BriefingTemplateRenderer.renderKrMarketNow(sheet, kospi = kospi, kosdaq = kosdaq, provisional = provisional, flowRequired = live)
        val name = when (market) { Market.KOSPI -> "코스피"; Market.KOSDAQ -> "코스닥"; null -> "국내 시장" }
        return MarketCard("$name · $md ${if (closed) "종가" else "장중"}", now, r.summary, r.sections)
    }

    /** netAmountEok: 순매수 금액(억원). 키움은 백만원 단위로 주므로 /100 (대시보드와 같은 변환) */
    data class FlowStock(val code: String, val name: String, val netAmountEok: Long, val periodChangeRate: Double?, val streakDays: Int?)
    /** 코스피+코스닥 전체 순위 (투자자별 목록 화면과 같음). basis: 집계 기간 ("10/15(목)" 또는 "10/13(월)~10/15(목)"). provisional: 장중 잠정치 */
    data class MarketInvestorFlow(
        val investor: Investor, val days: Int, val side: Side, val minStreakDays: Int?, val stocks: List<FlowStock>,
        val basis: String? = null, val provisional: Boolean = false,
    )

    /** 직전에 끝난 거래일 (투자자 수급 확정 기준 20:01) */
    private fun latestClosedKr(now: LocalDateTime = LocalDateTime.now()): LocalDate {
        val today = now.toLocalDate()
        return if (!holidayService.isHoliday(today) && !now.toLocalTime().isBefore(KR_FLOW_CLOSED)) today else holidayService.lastTradingDay(today.minusDays(1))
    }

    /**
     * 투자자별 순매수·순매도 상위. investor·side 는 필수(모르면 비서가 되묻는다).
     * - 당일(1일)·외국인/기관·연속 조건 없음: 투자자별 목록 화면과 같은 로직 (장중 잠정치 ka10063, 그 외 직전 거래일 확정치 ka10058)
     * - "N일 연속" 조건: ka10131 (순매수만 제공 — 연속 순매도·날짜 지정은 미지원)
     * - 그 밖(기간·날짜 지정·연기금·개인): ka10058 확정치를 기간 합산. 현재가가 조회일과 무관한 "지금" 값이라 등락률은 직전 거래일 1일 조회에서만 채운다
     */
    fun marketInvestorFlow(investor: Investor, days: Int, side: Side, minStreakDays: Int?, requested: LocalDate?): Any {
        val date = requested?.takeUnless { it == LocalDate.now() && !holidayService.isHoliday(it) }
        if (days !in RANK_PERIODS) throw ToolException("days 는 ${RANK_PERIODS.joinToString("/")} 중 하나여야 합니다")
        if (minStreakDays != null && investor !in listOf(Investor.FOREIGN, Investor.INSTITUTION)) throw ToolException("연속 순매수 조건은 외국인·기관만 지원합니다")
        if (minStreakDays != null && minStreakDays !in STREAK_DAYS_RANGE) throw ToolException("연속 순매수 조건은 ${STREAK_DAYS_RANGE.first}~${STREAK_DAYS_RANGE.last}일로 지정해 주세요")
        date?.let { d -> unavailableDay(StockMarket.KR, d)?.let { return TextReply(it) } }
        if (minStreakDays != null && side == Side.SELL) return TextReply("연속 순매도 조건은 지원하지 않습니다. 연속 순매수만 조회할 수 있습니다.")
        if (minStreakDays != null && date != null) return TextReply("연속 순매수 조건은 날짜를 지정해 조회할 수 없습니다. 날짜 없이 물으시면 최근 기준으로 보여 드립니다.")

        val markets = listOf("001", "101")   // 결과는 화면과 같이 전체. ka10058·ka10131 시장구분에 전체가 없어 두 번 불러 합친다
        fun md(d: LocalDate) = TemplateFormat.monthDay(d)
        fun amount(v: String?) = TemplateFormat.parseLong(v)

        // 1) 화면과 같은 당일 순위
        if (date == null && days == 1 && minStreakDays == null && investor in listOf(Investor.FOREIGN, Investor.INSTITUTION)) {
            val req = InvestorListReq(
                trdeTp = if (side == Side.BUY) InvestorService.TRDE_BUY else InvestorService.TRDE_SELL,
                orgnTp = if (investor == Investor.FOREIGN) InvestorService.ORGN_FOREIGN else InvestorService.ORGN_INSTITUTION,
            )
            val res = investorService.listInvestors(req)
                ?: throw ToolException("투자자별 순위 조회에 실패했습니다")
            val stocks = res.investorList.orEmpty().mapNotNull { i ->
                val amt = amount(i.netprpsAmt) ?: return@mapNotNull null
                FlowStock(i.stkCd?.substringBefore("_") ?: return@mapNotNull null, i.stkNm ?: "", amt / 100, TemplateFormat.parse(i.fluRt), null)
            }.take(MAX_FLOW_STOCKS)
            val basis = res.baseDate?.let { md(LocalDate.parse(it)) }
            return MarketInvestorFlow(investor, days, side, null, stocks, basis, res.provisional)
        }

        // 2) N일 연속 순매수 (ka10131)
        if (minStreakDays != null) {
            val stocks = markets.flatMap { m ->
                val res = runBlocking { investorClient.investorTradeRankList(KiwoomInvestorTradeRankListReq(dt = days.toString(), mrkt_tp = m, stk_inds_tp = "0", amt_qty_tp = "0", stex_tp = "3")) }
                res.orgn_frgnr_cont_trde_prst.orEmpty().mapNotNull { r ->
                    val amt = amount(if (investor == Investor.FOREIGN) r.frgnr_nettrde_amt else r.orgn_nettrde_amt) ?: return@mapNotNull null
                    val streak = amount(if (investor == Investor.FOREIGN) r.frgnr_cont_netprps_dys else r.orgn_cont_netprps_dys)?.toInt()
                    FlowStock(r.stk_cd?.substringBefore("_") ?: return@mapNotNull null, r.stk_nm ?: "", amt / 100, TemplateFormat.parse(r.prid_stkpc_flu_rt), streak)
                }
            }
                .filter { it.netAmountEok > 0 && (it.streakDays ?: 0) >= minStreakDays }
                .sortedByDescending { it.netAmountEok }
                .take(MAX_FLOW_STOCKS)
            val end = latestClosedKr()
            return MarketInvestorFlow(investor, days, side, minStreakDays, stocks, "${md(end)} 기준")
        }

        // 3) ka10058 확정치 (기간·날짜 지정·연기금·개인)
        val latest = latestClosedKr()
        val end = date ?: latest
        val start = periodResolver.tradingDaysBack(days, end)
        val invsrTp = when (investor) { Investor.FOREIGN -> "9000"; Investor.INSTITUTION -> "9999"; Investor.PENSION -> "6000"; Investor.INDIVIDUAL -> "8000" }
        val withRate = days == 1 && end == latest
        val stocks = markets.flatMap { m ->
            val res = runBlocking {
                investorClient.investorTradeDay(KiwoomInvestorTradeDayReq(
                    strt_dt = start.format(YYYYMMDD), end_dt = end.format(YYYYMMDD), trde_tp = if (side == Side.BUY) "2" else "1",
                    mrkt_tp = m, invsr_tp = invsrTp, stex_tp = "3",
                ))
            }
            res?.invsr_daly_trde_stk.orEmpty().mapNotNull { r ->
                val amt = amount(r.netslmt_amt) ?: return@mapNotNull null
                val rate = if (!withRate) null else {
                    val cur = TemplateFormat.parse(r.cur_prc)?.let { abs(it) }
                    val pred = TemplateFormat.parse(r.pred_pre)
                    if (cur != null && pred != null && cur - pred > 0) pred / (cur - pred) * 100 else null
                }
                FlowStock(r.stk_cd?.substringBefore("_") ?: return@mapNotNull null, r.stk_nm ?: "", (if (side == Side.BUY) abs(amt) else -abs(amt)) / 100, rate, null)
            }
        }
            .sortedByDescending { abs(it.netAmountEok) }
            .take(MAX_FLOW_STOCKS)
        val basis = if (start == end) md(end) else "${md(start)}~${md(end)}"
        return MarketInvestorFlow(investor, days, side, null, stocks, basis)
    }

    data class StockQuote(
        val code: String, val name: String, val market: StockMarket, val link: String, val currency: String,
        val price: Double, val changeRate: Double?, val changeAmount: Double?, val volume: Double?,
        val high: Double?, val low: Double?, val high52w: Double?, val low52w: Double?,
        val title: String = name,   // 카드 제목(상세형). 비교 표는 name(목록형)
        val basis: String? = null,  // 기준 표기 "10/15(수) 종가"·"10/17(금) 장중"·"현지 10/15(수) 종가". 코인 실시간은 null
        val session: SessionQuote? = null,  // 미국 데이·프리·애프터마켓 등락률 (토스 연동 회원만). 없으면 키움 값만 보인다
    )

    data class SessionQuote(val label: String, val regularClose: Double, val regularCloseDate: String, val changeAmount: Double, val changeRate: Double)

    /** 여러 종목 중 일부만 그날 조회할 수 없을 때 (예: 미국만 휴장) — 조회된 카드와 안내 문장을 함께 */
    data class QuoteAnswer(val quotes: List<StockQuote>, val notice: String)

    /** 시장별 "지금" 기준 거래일과 장중 여부 */
    private data class Session(val date: LocalDate, val open: Boolean)

    private fun krSession(now: LocalDateTime = LocalDateTime.now()): Session {
        val today = now.toLocalDate()
        val open = !holidayService.isHoliday(today) && !now.toLocalTime().isBefore(KR_OPEN)
        return Session(if (open) today else holidayService.lastTradingDay(today.minusDays(1)), open && now.toLocalTime().isBefore(KR_CLOSE))
    }

    private fun usSession(): Session {
        val nowEt = LocalDateTime.now().atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val d = nowEt.toLocalDate()
        val started = usMarketCalendarService.isUsTradingDay(d) && !nowEt.toLocalTime().isBefore(US_OPEN_ET)
        val date = if (started) d else usMarketCalendarService.lastClosedUsTradingDay()
        val open = started && nowEt.toLocalTime().isBefore(usMarketCalendarService.closeTimeEt(d))
        return Session(date, open)
    }

    private fun unavailableDay(market: StockMarket, date: LocalDate): String? {
        val md = TemplateFormat.monthDay(date)
        return when (market) {
            StockMarket.KR -> {
                val today = LocalDate.now()
                when {
                    date.isAfter(today) -> "${md}은 아직 장이 열리지 않았습니다."
                    holidayService.isHoliday(date) -> "${md}은 국내 증시 휴장일입니다."
                    date == today && !holidayService.isHoliday(today) && LocalTime.now().isBefore(KR_OPEN) -> "오늘 국내 장은 아직 열리지 않았습니다. (09:00 개장)"
                    else -> null
                }
            }
            StockMarket.US -> when {
                !usMarketCalendarService.isUsTradingDay(date) -> "미국 증시는 현지 ${md} 휴장일입니다."
                date.isAfter(usSession().date) -> "미국 증시 현지 ${md} 장은 아직 열리지 않았습니다."
                else -> null
            }
            StockMarket.CRYPTO -> if (date.isAfter(LocalDate.now())) "${md}은 아직 오지 않은 날짜입니다." else null
        }
    }

    /**
     * date 미지정 = 현재 시세. 지정하면 그날 일봉(종가·고가·저가·거래량, 전일 종가 대비 등락). 미국은 현지 날짜, 코인은 09:00~익일 09:00 일봉.
     */
    fun stockQuotes(queries: List<String>, memberId: Long, marketHint: StockMarket?, date: AssistantDate?): Any {
        if (queries.isEmpty()) throw ToolException("종목을 입력해 주세요")
        if (queries.size > MAX_STOCKS) throw ToolException("종목은 최대 ${MAX_STOCKS}개까지 비교할 수 있습니다")
        val stocks = queries.map { q -> stockResolver.resolve(q, memberId, marketHint) }
        // 종목마다 시장 기준으로 날짜를 정한다 (미국 "오늘"은 날짜 없이 = 지금 시세)
        fun dateOf(s: ResolvedStock) = date?.resolve(s.market)
        if (stocks.all { dateOf(it) == null }) return stocks.map { quote(it) }

        val notices = stocks.mapNotNull { s -> dateOf(s)?.let { unavailableDay(s.market, it) } }.distinct()
        val available = stocks.filter { s -> dateOf(s)?.let { unavailableDay(s.market, it) } == null }
        if (available.isEmpty()) return TextReply(notices.joinToString("\n"))
        val quotes = available.map { s ->
            val d = dateOf(s)
            if (d == null || isLiveDay(s.market, d)) quote(s) else dailyQuote(s, d)
        }
        return if (notices.isEmpty()) quotes else QuoteAnswer(quotes, notices.joinToString("\n"))
    }

    private fun isLiveDay(market: StockMarket, date: LocalDate): Boolean = when (market) {
        StockMarket.KR -> date == LocalDate.now()
        StockMarket.US -> date == usSession().date && usSession().open
        StockMarket.CRYPTO -> date == LocalDate.now()
    }

    private fun dailyQuote(s: ResolvedStock, date: LocalDate): StockQuote {
        val key = date.format(YYYYMMDD)
        val md = TemplateFormat.monthDay(date)
        fun p(v: String?) = TemplateFormat.parse(v)?.let { abs(it) }
        fun build(close: Double, prev: Double?, volume: Double?, high: Double?, low: Double?, currency: String, basis: String) = StockQuote(
            s.code, s.name, s.market, s.link, currency,
            price = close, changeRate = prev?.takeIf { it > 0 }?.let { (close / it - 1) * 100 }, changeAmount = prev?.let { close - it },
            volume = volume, high = high, low = low, high52w = null, low52w = null, title = s.title, basis = basis,
        )
        return when (s.market) {
            StockMarket.KR -> {
                val rows = runBlocking { stockChartClient.chartDayList(KiwoomStockChartDayReq(stk_cd = s.code, base_dt = key, upd_stkpc_tp = "1")) }
                    .stk_dt_pole_chart_qry.orEmpty()
                val i = rows.indexOfFirst { it.dt == key }.takeIf { it >= 0 } ?: throw ToolException("${s.name} ${md} 시세가 없습니다")
                val r = rows[i]
                build(p(r.cur_prc) ?: throw ToolException("${s.name} ${md} 종가가 없습니다"), rows.getOrNull(i + 1)?.let { p(it.cur_prc) },
                    p(r.trde_qty), p(r.high_pric), p(r.low_pric), "KRW", "$md 종가")
            }
            StockMarket.US -> {
                val rows = usStockChartClient.usStockDayChart(KiwoomUsStockChartReq(stex_tp = s.stexTp ?: "ND", stk_cd = s.code), stopAt = key)
                    .result_list.orEmpty()
                val i = rows.indexOfFirst { it.dt == key }.takeIf { it >= 0 } ?: throw ToolException("${s.name} 현지 ${md} 시세가 없습니다")
                val r = rows[i]
                build(p(r.cur_prc) ?: throw ToolException("${s.name} 현지 ${md} 종가가 없습니다"), rows.getOrNull(i + 1)?.let { p(it.cur_prc) },
                    p(r.acc_trde_qty), p(r.high_pric), p(r.low_pric), "USD", "현지 $md 종가")
            }
            StockMarket.CRYPTO -> {
                // 업비트 일봉은 KST 09:00 시작 → 그날 캔들은 다음 날 09:00(= UTC 00:00) 직전까지
                val to = "${date.plusDays(1)}T00:00:00"   // candle_date_time_utc 형식 (CryptoService 페이지 조회와 같음)
                val c = candleClient.getCandlesDays(s.code, count = 1, to = to).firstOrNull()
                    ?.takeIf { it.candle_date_time_kst?.startsWith(date.toString()) == true }
                    ?: throw ToolException("${s.name} ${md} 시세가 없습니다")
                build(c.trade_price ?: throw ToolException("${s.name} ${md} 종가가 없습니다"), c.prev_closing_price,
                    c.candle_acc_trade_volume, c.high_price, c.low_price, "KRW", "$md 일봉 (09:00~익일 09:00)")
            }
        }
    }

    private fun quote(s: ResolvedStock): StockQuote = currentQuote(s).let { q ->
        when (s.market) {
            StockMarket.KR -> krSession().let { q.copy(basis = "${TemplateFormat.monthDay(it.date)} ${if (it.open) "장중" else "기준"}") }
            StockMarket.US -> q.copy(
                basis = usTickBasis(s.stexTp ?: "ND", s.code)
                    ?: usSession().let { "현지 ${TemplateFormat.monthDay(it.date)} ${if (it.open) "장중" else "기준"}" },
                session = sessionQuote(s.code, q.price),
            )
            StockMarket.CRYPTO -> q
        }
    }

    private fun sessionQuote(code: String, price: Double): SessionQuote? =
        runCatching { runBlocking { usSessionBaseService.currentBases(listOf(code)) } }
            .onFailure { log.error(it) { "미국 장구분 기준 종가 조회 실패 $code" } }.getOrNull()
            ?.let { b ->
                b.closes[code]?.let { close ->
                    SessionQuote(b.session.label, close, TemplateFormat.monthDay(b.baseDate), price - close, (price / close - 1) * 100)
                }
            }

    private fun usTickBasis(stexTp: String, code: String): String? =
        runCatching { usStockChartClient.usStockLatestTick(KiwoomUsStockChartReq(stex_tp = stexTp, stk_cd = code)) }
            .onFailure { log.error(it) { "미국 최근 체결 조회 실패 $code" } }.getOrNull()
            ?.let { usMarketCalendarService.tickBasis(it.cntr_tm, it.bus_dt) }

    private fun currentQuote(s: ResolvedStock): StockQuote = when (s.market) {
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
                high = p(info.high_pric), low = p(info.low_pric), high52w = p(def?._250hgst), low52w = p(def?._250lwst), title = s.title,
            )
        }
        StockMarket.US -> {
            val r = usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = s.stexTp ?: "ND", stk_cd = s.code))
            fun p(v: String?) = TemplateFormat.parse(v)?.let { abs(it) }
            StockQuote(
                s.code, s.name, s.market, s.link, "USD",
                price = p(r.cur_prc) ?: throw ToolException("${s.name} 현재가가 없습니다"),
                changeRate = TemplateFormat.parse(r.flu_rt), changeAmount = TemplateFormat.parse(r.pred_pre), volume = p(r.acc_trde_qty),
                high = p(r.high_pric), low = p(r.low_pric), high52w = p(r.wk52_hgst_pric), low52w = p(r.wk52_lwst_pric), title = s.title,
            )
        }
        StockMarket.CRYPTO -> {
            val t = tickerClient.getTickers(s.code).firstOrNull() ?: throw ToolException("${s.name} 시세 조회에 실패했습니다")
            val price = t.trade_price ?: throw ToolException("${s.name} 현재가가 없습니다")
            val prev = t.prev_closing_price?.takeIf { it > 0 }
            StockQuote(
                s.code, s.name, s.market, s.link, "KRW",
                price = price, changeRate = prev?.let { (price / it - 1) * 100 }, changeAmount = prev?.let { price - it }, volume = t.acc_trade_volume_24h,
                high = t.high_price, low = t.low_price, high52w = t.highest_52_week_price, low52w = t.lowest_52_week_price, title = s.title,
            )
        }
    }

    /** 순매수 금액은 억원 (ka10059 amt_qty_tp=1 은 백만원 → /100). changeRate 는 ka10059 flu_rt 가 100배(+167 = +1.67%)라 /100 */
    data class StockFlowDay(val date: String, val foreignEok: Long?, val institutionEok: Long?, val individualEok: Long?, val close: Double?, val changeRate: Double?)
    data class StockInvestorFlow(
        val code: String, val name: String, val link: String, val days: List<StockFlowDay>,
        val foreignStreak: Int, val institutionStreak: Int,   // 최근일부터 같은 방향 연속일수. 양수=순매수, 음수=순매도
    )

    /** 국내만 (ka10059 금액 기준). 미국·코인은 종목별 수급 데이터가 없다 (Q-10) */
    /** date 지정 시 그날까지 거슬러 n일 (연속일수도 그날 기준). 미래·휴장일·개장 전이면 문장으로 답한다 */
    fun stockInvestorFlows(queries: List<String>, memberId: Long, days: Int, date: LocalDate?): Any {
        if (queries.isEmpty()) throw ToolException("종목을 입력해 주세요")
        if (queries.size > MAX_STOCKS) throw ToolException("종목은 최대 ${MAX_STOCKS}개까지 비교할 수 있습니다")
        date?.let { d -> unavailableDay(StockMarket.KR, d)?.let { return TextReply(it) } }
        val n = days.coerceIn(1, MAX_STOCK_FLOW_DAYS)
        val base = date ?: LocalDate.now()
        return queries.map { q ->
            val s = stockResolver.resolve(q, memberId)
            if (s.market != StockMarket.KR) throw ToolException("종목별 수급은 국내 주식만 조회할 수 있습니다: ${s.name}")
            val rows = runBlocking {
                stockClient.stockInvestor(KiwoomStockInvestorReq(dt = base.format(YYYYMMDD), stk_cd = s.assetCode, amt_qty_tp = "1", trde_tp = "0", unit_tp = "1"))
            }.stk_invsr_orgn?.filter { (it.dt ?: "") <= base.format(YYYYMMDD) }?.take(n) ?: emptyList()
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

    /** 미국 시장: 미국 마감 브리핑과 같은 지수 타일(나스닥·S&P500·다우·필라델피아 반도체) + 국채 + 환율 */
    fun globalIndexes(): MarketCard {
        val now = LocalDateTime.now()
        val sheet = marketFactSheetService.collectUsNow(now)
        if (sheet.indexes == null) throw ToolException("미국 지수 조회에 실패했습니다")
        val r = BriefingTemplateRenderer.renderUsMarketNow(sheet)
        // 기준일 표기: 장중이면 진행 중인 현지 거래일, 아니면(주말·개장 전 포함) 직전에 끝난 현지 거래일 종가
        val session = usSession()
        return MarketCard("미국 시장 · 현지 ${TemplateFormat.monthDay(session.date)} ${if (session.open) "장중" else "종가"}", now, r.summary, r.sections)
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
    /**
     * date 미지정 = 현재 추천(stock_pick). 지정하면 그날 산출한 추천 기록(stock_pick_history, 매일 22:00 산출 → 다음 거래일용).
     * 카드의 "M/D 기준" 과 같은 날짜 기준
     */
    fun recommendList(grade: String?, date: LocalDate?): Any {
        if (date == null) {
            val picks = stockPickRepository.findAllByOrderByStkCdAsc()
                .filter { grade == null || it.type.equals(grade, ignoreCase = true) }
                .map { RecommendItem(it.stkCd.substringBefore("_"), it.stkNm, it.type) }
            return RecommendList("추천 종목", stockPickHistoryRepository.findMaxPickDate(), picks)
        }
        // 날짜를 말하면 그날 22:00 산출분 ("오늘"도 날짜 — 오늘 22:00 산출분)
        val md = TemplateFormat.monthDay(date)
        val today = LocalDate.now()
        val prefix = if (date == today) "오늘 " else ""
        if (date.isAfter(today)) return TextReply("${md} 추천은 아직 산출되지 않았습니다.")
        if (holidayService.isHoliday(date)) return TextReply("$prefix${md}은 휴장일이라 추천이 없습니다.")
        val rows = stockPickHistoryRepository.findByPickDateBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay().minusNanos(1))
        if (rows.isEmpty()) {
            if (date == today && LocalTime.now().isBefore(RECOMMEND_TIME)) return TextReply("오늘 추천은 22:00에 산출됩니다.")
            return TextReply("$prefix${md} 추천 기록이 없습니다.")
        }
        val picks = rows
            .filter { grade == null || it.type.equals(grade, ignoreCase = true) }
            .sortedBy { it.stkCd }
            .map { RecommendItem(it.stkCd.substringBefore("_"), it.stkNm, it.type) }
        return RecommendList("추천 종목", rows.maxOf { it.pickDate }, picks)
    }

    data class NewsRow(val title: String, val link: String, val pubDate: String)

    fun searchNews(query: String, n: Int): List<NewsRow> {
        if (query.isBlank()) throw ToolException("검색어를 입력해 주세요")
        return newsService.searchNews(query, 1).items.take(n.coerceIn(1, MAX_NEWS)).map { NewsRow(it.title, it.link, it.pubDate) }
    }
}
