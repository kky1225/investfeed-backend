package com.example.investfeed.internal.assistant

import com.example.investfeed.domain.assistant.service.BriefingType
import com.example.investfeed.domain.notification.entity.PriceTargetDirection
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.DataToolService
import com.example.investfeed.internal.assistant.tool.DisplayToolService
import com.example.investfeed.internal.assistant.tool.PeriodResolver
import com.example.investfeed.internal.assistant.tool.StockMarket
import com.example.investfeed.internal.assistant.tool.ToolException
import com.example.investfeed.internal.assistant.tool.ToolResponse
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
) {
    private val log = KotlinLogging.logger {}

    private inline fun <T> tool(name: String, source: String? = null, block: () -> T): ToolResponse<T> = try {
        ToolResponse.ok(block(), source)
    } catch (e: ToolException) {
        ToolResponse.fail(e.message ?: "도구 오류", e.candidates)
    } catch (e: Exception) {
        log.error(e) { "비서 도구 실패: $name" }
        ToolResponse.fail("$name 조회에 실패했습니다: ${e.javaClass.simpleName}")
    }

    @GetMapping("get_market_summary")
    fun marketSummary(
        @RequestParam(defaultValue = "KOSPI") market: DataToolService.Market,
        @RequestParam(required = false) date: LocalDate?,
    ) = tool("get_market_summary", "키움 ka20001·수급") { data.marketSummary(market, date) }

    @GetMapping("get_market_investor_flow")
    fun marketInvestorFlow(
        @RequestParam investor: DataToolService.Investor,
        @RequestParam(defaultValue = "KOSPI") market: DataToolService.Market,
        @RequestParam(defaultValue = "1") days: Int,
        @RequestParam(defaultValue = "BUY") side: DataToolService.Side,
        @RequestParam(required = false) minStreakDays: Int?,
    ) = tool("get_market_investor_flow", "키움 ka10131/ka10058") { data.marketInvestorFlow(investor, market, days, side, minStreakDays) }

    @GetMapping("get_stock_quote")
    fun stockQuote(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam stockQuery: List<String>,
        @RequestParam(required = false) market: StockMarket?,
    ) = tool("get_stock_quote", "키움·업비트 시세") { data.stockQuotes(stockQuery, user.member.id, market) }

    @GetMapping("get_stock_investor_flow")
    fun stockInvestorFlow(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam stockQuery: List<String>,
        @RequestParam(defaultValue = "5") days: Int,
    ) = tool("get_stock_investor_flow", "키움 ka10059") { data.stockInvestorFlows(stockQuery, user.member.id, days) }

    @GetMapping("get_global_indexes")
    fun globalIndexes() = tool("get_global_indexes", "네이버 시장지표") { data.globalIndexes() }

    @GetMapping("get_calendar")
    fun calendar(
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        @RequestParam(required = false) keyword: String?,
    ) = tool("get_calendar", "경제 캘린더") { data.calendar(from, to, keyword) }

    @GetMapping("get_recommend_list")
    fun recommendList(@RequestParam(required = false) grade: String?) =
        tool("get_recommend_list", "추천 시스템 (시스템 분류)") { data.recommendList(grade) }

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

    data class CreatePriceAlertReq(val assetCode: String, val name: String, val market: StockMarket, val price: Long, val direction: PriceTargetDirection)

    @PostMapping("create_price_alert")
    fun createPriceAlert(@AuthenticationPrincipal user: CustomUserDetails, @RequestBody req: CreatePriceAlertReq) =
        tool("create_price_alert") { action.createPriceAlert(user.member, req.assetCode, req.name, req.market, req.price, req.direction) }
}
