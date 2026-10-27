package com.example.investfeed.internal.assistant

import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.internal.assistant.dto.req.BriefingToolReq
import com.example.investfeed.internal.assistant.dto.req.CalendarToolReq
import com.example.investfeed.internal.assistant.dto.req.CircuitBreakerReq
import com.example.investfeed.internal.assistant.dto.req.ExchangeRateReq
import com.example.investfeed.internal.assistant.dto.req.MarketInvestorFlowReq
import com.example.investfeed.internal.assistant.dto.req.MarketSummaryReq
import com.example.investfeed.internal.assistant.dto.req.MyHoldingReq
import com.example.investfeed.internal.assistant.dto.req.MyPnlReq
import com.example.investfeed.internal.assistant.dto.req.MyPortfolioReq
import com.example.investfeed.internal.assistant.dto.req.NewsSearchReq
import com.example.investfeed.internal.assistant.dto.req.PriceAlertPreviewReq
import com.example.investfeed.internal.assistant.dto.req.RecommendListReq
import com.example.investfeed.internal.assistant.dto.req.SectorPerformanceReq
import com.example.investfeed.internal.assistant.dto.req.StockInvestorFlowReq
import com.example.investfeed.internal.assistant.dto.req.StockQuoteReq
import com.example.investfeed.internal.assistant.dto.req.TradingRankReq
import com.example.investfeed.internal.assistant.dto.req.TreasuryYieldReq
import com.example.investfeed.internal.assistant.tool.ActionToolService
import com.example.investfeed.internal.assistant.tool.AssistantDate
import com.example.investfeed.internal.assistant.tool.CircuitBreakerToolService
import com.example.investfeed.internal.assistant.tool.DataToolService
import com.example.investfeed.internal.assistant.tool.DisplayToolService
import com.example.investfeed.internal.assistant.tool.ExchangeRateToolService
import com.example.investfeed.internal.assistant.tool.RankToolService
import com.example.investfeed.internal.assistant.tool.SectorToolService
import com.example.investfeed.internal.assistant.tool.StockMarket
import com.example.investfeed.internal.assistant.tool.ToolException
import com.example.investfeed.internal.assistant.tool.ToolResponse
import com.example.investfeed.internal.assistant.tool.TreasuryToolService
import mu.KotlinLogging
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
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
    private val rankTool: RankToolService,
    private val sectorTool: SectorToolService,
    private val exchangeRateTool: ExchangeRateToolService,
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
    fun marketSummary(@ModelAttribute req: MarketSummaryReq) =
        tool("get_market_summary", "키움 시세·수급") { data.marketSummary(req.market, AssistantDate.parse(req.date)?.resolve(StockMarket.KR)) }

    @GetMapping("get_circuit_breakers")
    fun circuitBreakers(@ModelAttribute req: CircuitBreakerReq) =
        tool("get_circuit_breakers", "서킷브레이커 판정·알림") { circuitBreakerTool.circuitBreakers(req.market, req.index, AssistantDate.parse(req.date)) }

    @GetMapping("get_market_investor_flow")
    fun marketInvestorFlow(@ModelAttribute req: MarketInvestorFlowReq) = tool("get_market_investor_flow", "키움 수급") {
        data.marketInvestorFlow(req.investor, req.days, req.side, req.minStreakDays, AssistantDate.parse(req.date)?.resolve(StockMarket.KR))
    }

    @GetMapping("get_trading_rank")
    fun tradingRank(@ModelAttribute req: TradingRankReq) =
        tool("get_trading_rank", "키움 순위") { rankTool.tradingRank(req.market, req.type, AssistantDate.parse(req.date)) }

    @GetMapping("get_sector_performance")
    fun sectorPerformance(@ModelAttribute req: SectorPerformanceReq) =
        tool("get_sector_performance", "키움 업종") { sectorTool.sectorPerformance(req.market, req.sector, req.multiDay, req.sort, AssistantDate.parse(req.date)) }

    @GetMapping("get_stock_quote")
    fun stockQuote(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: StockQuoteReq,
    ) = tool("get_stock_quote") { data.stockQuotes(req.stockQuery, user.member.id, req.market, AssistantDate.parse(req.date)) }
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
        @ModelAttribute req: StockInvestorFlowReq,
    ) = tool("get_stock_investor_flow", "키움 수급") {
        data.stockInvestorFlows(req.stockQuery, user.member.id, req.days, AssistantDate.parse(req.date)?.resolve(StockMarket.KR))
    }

    @GetMapping("get_global_indexes")
    fun globalIndexes() = tool("get_global_indexes", "네이버 시장지표·국채") { data.globalIndexes() }

    @GetMapping("get_treasury_yields")
    fun treasuryYields(@ModelAttribute req: TreasuryYieldReq) =
        tool("get_treasury_yields", "네이버·FRED·ECOS 국채") { treasuryTool.treasuryYields(req.market, AssistantDate.parse(req.date)) }

    @GetMapping("get_exchange_rates")
    fun exchangeRates(@ModelAttribute req: ExchangeRateReq) =
        tool("get_exchange_rates", "네이버 시장지표 · 하나은행 고시") { exchangeRateTool.exchangeRates(AssistantDate.parse(req.date)) }

    @GetMapping("get_calendar")
    fun calendar(@ModelAttribute req: CalendarToolReq) =
        tool("get_calendar", "경제 캘린더") { data.calendar(req.from, req.to, req.keyword, req.type, req.country) }

    @GetMapping("get_recommend_list")
    fun recommendList(@ModelAttribute req: RecommendListReq) =
        tool("get_recommend_list", "추천 시스템") { data.recommendList(req.grade, AssistantDate.parse(req.date)?.resolve(StockMarket.KR)) }

    @GetMapping("search_news")
    fun searchNews(@ModelAttribute req: NewsSearchReq) =
        tool("search_news", "네이버 뉴스") { data.searchNews(req.query, req.n) }

    // ── 표시 도구 (카드 참조만) ──

    @GetMapping("show_my_portfolio")
    fun myPortfolio(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: MyPortfolioReq,
    ) = tool("show_my_portfolio") {
        display.myPortfolio(user.member, req.assetClass, DisplayToolService.HoldingFilter(req.dayRateLt, req.dayRateGt, req.totalRateLt, req.totalRateGt))
    }

    @GetMapping("show_my_holding")
    fun myHolding(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: MyHoldingReq,
    ) = tool("show_my_holding") { display.myHolding(user.member, req.stockQuery) }

    @GetMapping("show_my_pnl")
    fun myPnl(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: MyPnlReq,
    ) = tool("show_my_pnl") { display.myPnl(user.member, req.period, req.assetClass) }

    @GetMapping("show_briefing")
    fun briefing(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: BriefingToolReq,
    ) = tool("show_briefing") { display.briefing(user.member, req.date ?: LocalDate.now(), req.type) }

    @GetMapping("create_price_alert/preview")
    fun previewPriceAlert(
        @AuthenticationPrincipal user: CustomUserDetails,
        @ModelAttribute req: PriceAlertPreviewReq,
    ) = tool("create_price_alert") { action.previewPriceAlert(user.member, req.stockQuery, req.price, req.direction) }
}
