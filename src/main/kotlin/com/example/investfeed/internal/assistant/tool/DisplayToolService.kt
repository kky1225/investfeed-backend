package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.dto.factsheet.BrokerHoldingsFact
import com.example.investfeed.domain.assistant.dto.factsheet.HoldingFact
import com.example.investfeed.domain.assistant.dto.factsheet.PersonalFactSheet
import com.example.investfeed.domain.assistant.dto.message.Section
import com.example.investfeed.domain.assistant.service.BriefingTemplateRenderer
import com.example.investfeed.domain.assistant.dto.message.MessageType
import com.example.investfeed.domain.assistant.repository.AssistantMessageRepository
import com.example.investfeed.domain.assistant.service.BriefingType
import com.example.investfeed.domain.assistant.service.PersonalBriefingService
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DisplayToolService(
    private val personalBriefingService: PersonalBriefingService,
    private val stockResolver: StockResolver,
    private val periodResolver: PeriodResolver,
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val assistantMessageRepository: AssistantMessageRepository,
    private val cardStore: CardStore,
) {
    enum class AssetClass { KR, US, CRYPTO }

    companion object {
        const val KIND_PORTFOLIO = "PORTFOLIO"
        const val KIND_HOLDING = "HOLDING"
        const val KIND_PNL = "REALIZED_PNL"
        private const val TOP_N = 3
    }

    data class HoldingRow(val code: String, val name: String, val link: String?, val broker: String, val currency: String,
                          val eval: Double?, val dayRate: Double?, val dayChange: Double?, val totalRate: Double?, val evalProfit: Double?)
    data class ClassSummary(val assetClass: AssetClass, val currency: String, val eval: Double, val dayChange: Double?, val dayRate: Double?, val totalRate: Double?,
                            val brokers: List<String>, val failedBrokers: List<String>)
    data class PortfolioCard(
        val asOf: LocalDateTime, val totalEvalKrw: Double?, val usdKrw: Double?,
        val classes: List<ClassSummary>, val topGainers: List<HoldingRow>, val topLosers: List<HoldingRow>,
        val filter: HoldingFilter?, val filtered: List<HoldingRow>?, val realizedMonthWon: Long?,
        val sections: List<Section> = emptyList(),
    )
    data class HoldingFilter(val dayRateLt: Double? = null, val dayRateGt: Double? = null, val totalRateLt: Double? = null, val totalRateGt: Double? = null) {
        fun hasCondition() = dayRateLt != null || dayRateGt != null || totalRateLt != null || totalRateGt != null
        fun matches(r: HoldingRow): Boolean {
            fun lt(v: Double?, bound: Double?) = bound == null || (v != null && v < bound)
            fun gt(v: Double?, bound: Double?) = bound == null || (v != null && v > bound)
            return lt(r.dayRate, dayRateLt) && gt(r.dayRate, dayRateGt) && lt(r.totalRate, totalRateLt) && gt(r.totalRate, totalRateGt)
        }
    }

    fun myPortfolio(member: Member, assetClass: AssetClass?, filter: HoldingFilter?): CardRef {
        val sheet = personalBriefingService.buildAllHoldingsSheet(member, LocalDate.now())
        val usdKrw = sheet.usdKrw
        val classes = listOfNotNull(
            sheet.krBrokers?.let { AssetClass.KR to it },
            sheet.usBrokers?.let { AssetClass.US to it },
            sheet.coinExchanges?.let { AssetClass.CRYPTO to it },
        ).filter { (c, _) -> assetClass == null || c == assetClass }

        val rows = classes.flatMap { (c, brokers) -> brokers.flatMap { b -> b.items.map { row(c, b, it) } } }
        val summaries = classes.map { (c, brokers) ->
            val ok = brokers.filter { !it.failed }
            val eval = ok.sumOf { it.eval }
            val dayChange = ok.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
            ClassSummary(
                c, if (c == AssetClass.US) "USD" else "KRW", eval, dayChange,
                dayChange?.let { d -> (eval - d).takeIf { it > 0 }?.let { d / it * 100 } },
                ok.mapNotNull { it.totalRate }.takeIf { it.isNotEmpty() }?.average(),
                brokers.map { it.brokerName }, brokers.filter { it.failed }.map { it.brokerName },
            )
        }
        val totalKrw = summaries.sumOf { s -> if (s.currency == "USD") (usdKrw?.let { s.eval * it } ?: 0.0) else s.eval }.takeIf { summaries.isNotEmpty() }
        val withRate = rows.filter { it.dayRate != null }
        val card = PortfolioCard(
            asOf = LocalDateTime.now(), totalEvalKrw = totalKrw, usdKrw = usdKrw, classes = summaries,
            topGainers = withRate.sortedByDescending { it.dayRate }.take(TOP_N),
            topLosers = withRate.sortedBy { it.dayRate }.take(TOP_N),
            filter = filter?.takeIf { it.hasCondition() },
            filtered = filter?.takeIf { it.hasCondition() }?.let { f -> rows.filter { f.matches(it) }.sortedBy { it.dayRate } },
            realizedMonthWon = sheet.realized?.monthTotalWon,
            sections = accountSections(sheet, assetClass, filter?.takeIf { it.hasCondition() }),
        )
        return cardStore.put(member.id, KIND_PORTFOLIO, card)
    }

    private fun accountSections(sheet: PersonalFactSheet, assetClass: AssetClass?, filter: HoldingFilter?): List<Section> {
        fun keep(c: AssetClass, brokers: List<BrokerHoldingsFact>?) = brokers?.let { bs ->
            if (filter == null) bs
            else bs.map { b -> b.copy(items = b.items.filter { filter.matches(row(c, b, it)) }) }.filter { it.failed || it.items.isNotEmpty() }
        }
        val shown = sheet.copy(
            krBrokers = keep(AssetClass.KR, sheet.krBrokers),
            usBrokers = keep(AssetClass.US, sheet.usBrokers),
            coinExchanges = keep(AssetClass.CRYPTO, sheet.coinExchanges),
        )
        val scoped = if (assetClass == null) shown else shown.copy(
            krBrokers = shown.krBrokers.takeIf { assetClass == AssetClass.KR }.orEmpty(),
            usBrokers = shown.usBrokers.takeIf { assetClass == AssetClass.US }.orEmpty(),
            coinExchanges = shown.coinExchanges.takeIf { assetClass == AssetClass.CRYPTO }.orEmpty(),
        )
        return BriefingTemplateRenderer.accountSectionsByBroker(scoped)
    }

    private fun row(c: AssetClass, b: BrokerHoldingsFact, h: HoldingFact) =
        HoldingRow(h.code, h.name, h.link, b.brokerName, b.currency, h.eval, h.dayRate, h.dayChange, h.totalRate, h.evalProfit)

    data class HoldingCardBroker(val broker: String, val qty: Double?, val purPrice: Double?, val eval: Double?, val evalProfit: Double?, val totalRate: Double?)
    data class HoldingCard(
        val asOf: LocalDateTime, val code: String, val name: String, val market: StockMarket, val link: String, val currency: String,
        val curPrc: Double?, val dayRate: Double?, val dayChange: Double?, val brokers: List<HoldingCardBroker>,
    )

    fun myHolding(member: Member, stockQuery: String): CardRef {
        val s = stockResolver.resolve(stockQuery, member.id)
        val sheet = personalBriefingService.buildAllHoldingsSheet(member, LocalDate.now())
        val brokers = when (s.market) { StockMarket.KR -> sheet.krBrokers; StockMarket.US -> sheet.usBrokers; StockMarket.CRYPTO -> sheet.coinExchanges } ?: emptyList()
        val hits = brokers.flatMap { b -> b.items.filter { it.code == s.code }.map { b to it } }
        if (hits.isEmpty()) throw ToolException("${s.name}은(는) 보유하고 있지 않습니다")
        val first = hits.first().second
        val card = HoldingCard(
            asOf = LocalDateTime.now(), code = s.code, name = s.name, market = s.market, link = s.link,
            currency = if (s.market == StockMarket.US) "USD" else "KRW",
            curPrc = first.curPrc, dayRate = first.dayRate, dayChange = hits.mapNotNull { it.second.dayChange }.takeIf { it.isNotEmpty() }?.sum(),
            brokers = hits.map { (b, h) -> HoldingCardBroker(b.brokerName, h.qty, h.purPrice, h.eval, h.evalProfit, h.totalRate) },
        )
        return cardStore.put(member.id, KIND_HOLDING, card)
    }

    data class PnlRow(val broker: String, val market: MarketType, val year: Int, val month: Int, val realizedPnl: Long)
    data class PnlCard(val period: PeriodResolver.Period, val year: Int, val month: Int?, val totalWon: Long, val byClass: Map<String, Long>, val rows: List<PnlRow>)

    fun myPnl(member: Member, period: PeriodResolver.Period, assetClass: AssetClass?): CardRef {
        val (year, month) = periodResolver.resolve(period)
        val markets = when (assetClass) { null -> listOf(MarketType.STOCK, MarketType.CRYPTO); AssetClass.CRYPTO -> listOf(MarketType.CRYPTO); else -> listOf(MarketType.STOCK) }
        val rows = markets.flatMap { m ->
            (if (month != null) memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearAndMonthOrderByYearDescMonthDesc(member.id, m, year, month)
             else memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearOrderByYearDescMonthDesc(member.id, m, year))
                .map { PnlRow(it.broker.name, m, it.year, it.month, it.realizedPnl) }
        }
        val card = PnlCard(period, year, month, rows.sumOf { it.realizedPnl }, rows.groupBy { it.market.name }.mapValues { (_, v) -> v.sumOf { it.realizedPnl } }, rows)
        return cardStore.put(member.id, KIND_PNL, card)
    }

    data class BriefingPointer(val messageId: Long, val headline: String, val createdAt: LocalDateTime, val type: BriefingType)

    fun briefing(member: Member, date: LocalDate, type: BriefingType): BriefingPointer {
        val m = assistantMessageRepository.findFirstByMemberIdAndTypeAndSubtypeAndCreatedAtBetweenOrderByIdDesc(
            member.id, MessageType.BRIEFING.name, type.subtype, date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
        ) ?: throw ToolException("$date 의 ${type.name} 브리핑이 없습니다")
        return BriefingPointer(m.id, m.headlineText, m.createdAt, type)
    }
}