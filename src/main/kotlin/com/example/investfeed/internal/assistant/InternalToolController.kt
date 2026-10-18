package com.example.investfeed.internal.assistant

import com.example.investfeed.domain.assistant.service.BriefingType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.CircuitBreakerToolService
import com.example.investfeed.internal.assistant.tool.DataToolService
import com.example.investfeed.internal.assistant.tool.DisplayToolService
import com.example.investfeed.internal.assistant.tool.PeriodResolver
import com.example.investfeed.internal.assistant.tool.StockMarket
import com.example.investfeed.internal.assistant.tool.ToolException
import com.example.investfeed.internal.assistant.tool.ToolResponse
import com.example.investfeed.internal.assistant.tool.TreasuryToolService
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/internal/assistant/tools")
class InternalToolController(
    private val data: DataToolService,
    private val display: DisplayToolService,
    private val action: ActionToolService,
    private val circuitBreakerTool: CircuitBreakerToolService,
    private val treasuryTool: TreasuryToolService,
) {
    private val log = KotlinLogging.logger {}

    private inline fun <T> tool(name: String, source: String? = null, block: () -> T): ToolResponse<T> = try {
        ToolResponse.ok(block(), source)
    } catch (e: ToolException) {
        ToolResponse.fail(e.message ?: "도구 오류", e.candidates, e.query)
    } catch (e: Exception) {
        log.error(e) { "비서 도구 실패: $name" }
        ToolResponse.fail("$name 조회에 실패했습니다: ${e.javaClass.simpleName}")
    }

    @GetMapping("get_market_summary")
    fun marketSummary(
        @RequestParam(required = false) market: DataToolService.Market?,   // 미지정 = 국내 지수 전체 (코스피·코스닥)
        @RequestParam(required = false) date: LocalDate?,
    ) = tool("get_market_summary", "키움 시세·수급") { data.marketSummary(market, date) }

    @GetMapping("get_circuit_breakers")
    fun circuitBreakers(
        @RequestParam market: CircuitBreakerToolService.CbMarket,
        @RequestParam(required = false) index: CircuitBreakerToolService.CbIndex?,   // 지수를 직접 물었을 때 (국내는 그 지수만, 미국은 안내 문구만)
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 오늘(휴장일이면 직전 거래일)
    ) = tool("get_circuit_breakers", "서킷브레이커 판정·알림") { circuitBreakerTool.circuitBreakers(market, index, date) }

    @GetMapping("get_market_investor_flow")
    fun marketInvestorFlow(
        @RequestParam investor: DataToolService.Investor,
        @RequestParam(defaultValue = "1") days: Int,
        @RequestParam side: DataToolService.Side,   // 순매수/순매도 필수 — 질문에 없으면 비서가 되묻는다
        @RequestParam(required = false) minStreakDays: Int?,
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 최근 기준. 지정하면 그날까지 days 거래일
    ) = tool("get_market_investor_flow", "키움 수급") { data.marketInvestorFlow(investor, days, side, minStreakDays, date) }

    @GetMapping("get_stock_quote")
    fun stockQuote(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam stockQuery: List<String>,
        @RequestParam(required = false) market: StockMarket?,
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 현재 시세. 미국은 현지 날짜
    ) = tool("get_stock_quote") { data.stockQuotes(stockQuery, user.member.id, market, date) }
        .let { r ->
            val quotes = when (val d = r.data) {
                is List<*> -> d.filterIsInstance<DataToolService.StockQuote>()
                is DataToolService.QuoteAnswer -> d.quotes
                else -> null
            }
            r.copy(source = quotes?.let { quoteSource(it) })
        }

    private fun quoteSource(quotes: List<DataToolService.StockQuote>): String? =
        listOfNotNull(
            "키움".takeIf { quotes.any { it.market != StockMarket.CRYPTO } },
            "업비트".takeIf { quotes.any { it.market == StockMarket.CRYPTO } },
        ).takeIf { it.isNotEmpty() }?.joinToString("·", postfix = " 시세")

    @GetMapping("get_stock_investor_flow")
    fun stockInvestorFlow(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam stockQuery: List<String>,
        @RequestParam(defaultValue = "5") days: Int,
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 오늘까지
    ) = tool("get_stock_investor_flow", "키움 수급") { data.stockInvestorFlows(stockQuery, user.member.id, days, date) }

    @GetMapping("get_global_indexes")
    fun globalIndexes() = tool("get_global_indexes", "네이버 시장지표·국채") { data.globalIndexes() }

    @GetMapping("get_treasury_yields")
    fun treasuryYields(
        @RequestParam(required = false) market: TreasuryToolService.TreasuryMarket?,   // 미지정 = 한국·미국 둘 다
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 당일(네이버). 지정 = 그날 종가(미국 FRED·한국 ECOS)
    ) = tool("get_treasury_yields", "네이버·FRED·ECOS 국채") { treasuryTool.treasuryYields(market, date) }

    @GetMapping("get_calendar")
    fun calendar(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) type: DataToolService.CalendarType?,
        @RequestParam(required = false) country: DataToolService.CalendarCountry?,
    ) = tool("get_calendar", "경제 캘린더") { data.calendar(from, to, keyword, type, country) }

    @GetMapping("get_recommend_list")
    fun recommendList(
        @RequestParam(required = false) grade: String?,
        @RequestParam(required = false) date: LocalDate?,   // 미지정 = 현재 추천. 지정하면 그날 산출 기록
    ) = tool("get_recommend_list", "추천 시스템") { data.recommendList(grade, date) }

    @GetMapping("search_news")
    fun searchNews(@RequestParam query: String, @RequestParam(defaultValue = "5") n: Int) =
        tool("search_news", "네이버 뉴스") { data.searchNews(query, n) }

    // ── 표시 도구 (카드 참조만) ──

    @GetMapping("show_my_portfolio")
    fun myPortfolio(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam(required = false) assetClass: DisplayToolService.AssetClass?,
        @RequestParam(required = false) dayRateLt: Double?,
        @RequestParam(required = false) dayRateGt: Double?,
        @RequestParam(required = false) totalRateLt: Double?,
        @RequestParam(required = false) totalRateGt: Double?,
    ) = tool("show_my_portfolio") { display.myPortfolio(user.member, assetClass, DisplayToolService.HoldingFilter(dayRateLt, dayRateGt, totalRateLt, totalRateGt)) }

    @GetMapping("show_my_holding")
    fun myHolding(@AuthenticationPrincipal user: CustomUserDetails, @RequestParam stockQuery: String) =
        tool("show_my_holding") { display.myHolding(user.member, stockQuery) }

    @GetMapping("show_my_pnl")
    fun myPnl(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam(defaultValue = "THIS_MONTH") period: PeriodResolver.Period,
        @RequestParam(required = false) assetClass: DisplayToolService.AssetClass?,
    ) = tool("show_my_pnl") { display.myPnl(user.member, period, assetClass) }

    @GetMapping("show_briefing")
    fun briefing(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam(required = false) date: LocalDate?,
        @RequestParam(defaultValue = "KR_CLOSE") type: BriefingType,
    ) = tool("show_briefing") { display.briefing(user.member, date ?: LocalDate.now(), type) }

    @GetMapping("create_price_alert/preview")
    fun previewPriceAlert(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam stockQuery: String,
        @RequestParam price: Long,
        @RequestParam direction: PriceTargetDirection,
    ) = tool("create_price_alert") { action.previewPriceAlert(user.member, stockQuery, price, direction) }
}
