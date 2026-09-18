package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.entity.MemberBroker
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.holding.service.CryptoHoldingService
import com.example.investfeed.domain.holding.service.CryptoManualHoldingService
import com.example.investfeed.domain.holding.service.HoldingService
import com.example.investfeed.domain.holding.service.ManualHoldingService
import com.example.investfeed.domain.holding.service.TossHoldingService
import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import com.example.investfeed.domain.security.CustomUserDetailsService
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PersonalBriefingService(
    private val memberBrokerRepository: MemberBrokerRepository,
    private val brokerRepository: BrokerRepository,
    private val holdingService: HoldingService,
    private val tossHoldingService: TossHoldingService,
    private val manualHoldingService: ManualHoldingService,
    private val cryptoHoldingService: CryptoHoldingService,
    private val cryptoManualHoldingService: CryptoManualHoldingService,
    private val usStockClient: UsStockClient,
    private val marketIndexService: MarketIndexService,
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val customUserDetailsService: CustomUserDetailsService,
    private val marketFactSheetService: MarketFactSheetService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val TOSS = "토스증권"
    }

    fun buildPersonalSheet(member: Member, type: BriefingType, tradeDate: LocalDate): PersonalFactSheet = withMember(member) {
        when (type) {
            BriefingType.KR_HOLDINGS -> buildKrHoldingsSheet(member)
            BriefingType.KR_CLOSE, BriefingType.KR_PRE -> PersonalFactSheet(memberId = member.id)   // 16:00·07:00 은 시장 파트만 (2026-09-16)
            BriefingType.US_CLOSE -> buildUsCloseSheet(member)
            BriefingType.COIN_DAILY -> PersonalFactSheet(memberId = member.id, coinExchanges = fetchCryptoHoldings(member, tradeDate))
        }
    }

    private fun buildKrHoldingsSheet(member: Member): PersonalFactSheet {
        val stock = fetchStockHoldings(member)
        return PersonalFactSheet(
            memberId = member.id,
            krBrokers = stock.map { (mb, res) -> toKrHoldingsFact(mb, res) },
            realized = runCatching { fetchRealizedProfit(member.id) }.onFailure { log.error(it) { "실현손익 조회 실패 memberId=${member.id}" } }.getOrNull(),
        )
    }

    private fun buildUsCloseSheet(member: Member): PersonalFactSheet {
        val stock = fetchStockHoldings(member)
        val blocks = stock.mapNotNull { (mb, res) -> toUsHoldingsFact(mb, res) }
        return PersonalFactSheet(
            memberId = member.id,
            usBrokers = blocks,
            usdKrw = marketIndexService.getMarketIndex(MarketIndexType.USD_KRW)?.price?.let { TemplateFormat.parse(it) },
        )
    }

    private fun fetchStockHoldings(member: Member): List<Pair<MemberBroker, HoldingListRes?>> =
        memberBrokerRepository.findByMemberIdOrderByOrderIndex(member.id)
            .filter { it.broker.market == MarketType.STOCK }
            .map { mb ->
                mb to runCatching {
                    when (mb.broker.type) {
                        BrokerType.API -> runBlocking { if (mb.broker.name == TOSS) tossHoldingService.listTossHoldingsSuspend() else holdingService.listHoldingsSuspend() }
                        BrokerType.MANUAL -> manualToHoldingList(mb)
                    }
                }.onFailure { log.error(it) { "보유 조회 실패 member=${member.loginId} broker=${mb.broker.name}" } }.getOrNull()
            }

    private fun manualToHoldingList(mb: MemberBroker): HoldingListRes {
        val res = manualHoldingService.listManualHoldings(mb.id)
        val items = res.holdings.map { h ->
            val cur = h.curPrc.toLongOrNull() ?: 0L
            val eval = cur * h.quantity
            HoldingItem(
                id = h.id, stkCd = h.stkCd, stkNm = h.stkNm, curPrc = h.curPrc,
                purPric = h.purPrice.toString(), purAmt = h.purAmt.toString(), evltAmt = eval.toString(),
                evltvPrft = (eval - h.purAmt).toString(),
                prftRt = if (h.purAmt > 0) String.format("%.2f", (eval - h.purAmt).toDouble() / h.purAmt * 100) else "0",
                rmndQty = h.quantity.toString(), possRt = "0", predClosePric = h.basePric,
            )
        }
        return HoldingListRes(
            totPurAmt = items.sumOf { it.purAmt.toLongOrNull() ?: 0L }.toString(),
            totEvltAmt = items.sumOf { it.evltAmt.toLongOrNull() ?: 0L }.toString(),
            totEvltPl = items.sumOf { it.evltvPrft.toLongOrNull() ?: 0L }.toString(),
            totPrftRt = "0", balance = res.balance.toString(), holdingList = items,
        )
    }

    private fun toKrHoldingsFact(mb: MemberBroker, res: HoldingListRes?): BrokerHoldingsFact {
        res ?: return BrokerHoldingsFact(mb.broker.name, "KRW", 0.0, null, null, null, emptyList(), failed = true)
        val items = res.holdingList.filter { it.stexTp == null }.map { h ->
            val cur = TemplateFormat.parse(h.curPrc) ?: 0.0
            val prev = TemplateFormat.parse(h.predClosePric)?.takeIf { it > 0 }
            val qty = TemplateFormat.parse(h.rmndQty) ?: 0.0
            val dayPl = TemplateFormat.parse(h.dayPl)?.takeIf { it != 0.0 }
            val eval = TemplateFormat.parse(h.evltAmt)
            val dayChange = when {
                prev != null -> qty * (cur - prev)
                dayPl != null -> dayPl
                else -> null
            }
            val dayRate = when {
                prev != null -> (cur / prev - 1) * 100
                dayPl != null && eval != null && eval - dayPl > 0 -> dayPl / (eval - dayPl) * 100
                else -> null
            }
            val code = h.stkCd.substringBefore("_")
            HoldingFact(code = code, name = h.stkNm, link = "/stock/detail/$code", dayRate = dayRate, dayChange = dayChange, totalRate = TemplateFormat.parse(h.prftRt), eval = eval,
                curPrc = cur, evalProfit = TemplateFormat.parse(h.evltvPrft))
        }
        val eval = items.sumOf { it.eval ?: 0.0 }
        val dayChange = items.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
        val dayRate = dayChange?.let { c -> (eval - c).takeIf { it > 0 }?.let { c / it * 100 } }
        val purAmt = res.holdingList.filter { it.stexTp == null }.sumOf { it.purAmt.toDoubleOrNull() ?: 0.0 }
        return BrokerHoldingsFact(mb.broker.name, "KRW", eval, dayChange, dayRate, if (purAmt > 0) (eval / purAmt - 1) * 100 else null, items)
    }

    private fun toUsHoldingsFact(mb: MemberBroker, res: HoldingListRes?): BrokerHoldingsFact? {
        res ?: return BrokerHoldingsFact(mb.broker.name, "USD", 0.0, null, null, null, emptyList(), failed = true)
        val usRows = res.holdingList.filter { it.stexTp != null }
        if (usRows.isEmpty()) return null
        val items = usRows.map { h ->
            val ticker = h.usStkCd ?: h.stkCd.substringBefore("_")
            val evalUsd = TemplateFormat.parse(h.evltAmtUsd)
            val dayRate = runCatching { TemplateFormat.parse(usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = h.stexTp!!, stk_cd = ticker)).flu_rt) }
                .onFailure { log.error(it) { "미국 종목 시세 조회 실패 $ticker" } }.getOrNull()
            val dayChange = if (dayRate != null && evalUsd != null && (1 + dayRate / 100) > 0) evalUsd - evalUsd / (1 + dayRate / 100) else null
            HoldingFact(code = ticker, name = h.stkNm.ifBlank { ticker }, link = "/us-stock/detail/${h.stexTp}/$ticker", dayRate = dayRate, dayChange = dayChange, totalRate = TemplateFormat.parse(h.prftRt), eval = evalUsd,
                curPrc = TemplateFormat.parse(h.curPrcUsd), evalProfit = TemplateFormat.parse(h.evltvPrftUsd))
        }
        val eval = items.sumOf { it.eval ?: 0.0 }
        val dayChange = items.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
        val dayRate = dayChange?.let { c -> (eval - c).takeIf { it > 0 }?.let { c / it * 100 } }
        val purUsd = usRows.sumOf { TemplateFormat.parse(it.purPricUsd)?.let { p -> p * (TemplateFormat.parse(it.rmndQty) ?: 0.0) } ?: 0.0 }
        return BrokerHoldingsFact(mb.broker.name, "USD", eval, dayChange, dayRate, if (purUsd > 0) (eval / purUsd - 1) * 100 else null, items)
    }

    private fun fetchCryptoHoldings(member: Member, dayDate: LocalDate): List<BrokerHoldingsFact> =
        memberBrokerRepository.findByMemberIdOrderByOrderIndex(member.id)
            .filter { it.broker.market == MarketType.CRYPTO }
            .map { mb ->
                runCatching {
                    val res = when (mb.broker.type) {
                        BrokerType.API -> cryptoHoldingService.listCryptoHoldings()
                        BrokerType.MANUAL -> cryptoManualToHoldingList(mb)
                    }
                    val items = res.holdingList.map { h ->
                        val qty = TemplateFormat.parse(h.rmndQty) ?: 0.0
                        val name = h.stkNm.ifBlank { h.stkCd.removePrefix("KRW-") }
                        val day = runCatching { marketFactSheetService.coinDay(h.stkCd, name, dayDate) }.getOrNull()
                        val eval = day?.let { it.close * qty }
                        val dayChange = day?.prevClose?.let { pc -> (day.close - pc) * qty }
                        HoldingFact(code = h.stkCd, name = name, link = "/crypto/detail/${h.stkCd}", dayRate = day?.changeRate, dayChange = dayChange, totalRate = TemplateFormat.parse(h.prftRt), eval = eval,
                            curPrc = day?.close, evalProfit = eval?.let { e -> TemplateFormat.parse(h.purAmt)?.let { e - it } })
                    }
                    val eval = items.sumOf { it.eval ?: 0.0 }
                    val dayChange = items.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
                    BrokerHoldingsFact(mb.broker.name, "KRW", eval, dayChange, dayChange?.let { c -> (eval - c).takeIf { it > 0 }?.let { c / it * 100 } }, TemplateFormat.parse(res.totPrftRt), items)
                }.onFailure { log.error(it) { "코인 보유 조회 실패 member=${member.loginId} broker=${mb.broker.name}" } }
                    .getOrElse { BrokerHoldingsFact(mb.broker.name, "KRW", 0.0, null, null, null, emptyList(), failed = true) }
            }

    private fun cryptoManualToHoldingList(mb: MemberBroker): HoldingListRes {
        val res = cryptoManualHoldingService.listCryptoManualHoldings(mb.id)
        val items = res.holdings.map { h ->
            val cur = h.curPrc.toLongOrNull() ?: 0L
            val eval = cur * h.quantity
            HoldingItem(id = h.id, stkCd = h.stkCd, stkNm = h.stkNm, curPrc = h.curPrc, purPric = h.purPrice.toString(), purAmt = h.purAmt.toString(),
                evltAmt = eval.toString(), evltvPrft = (eval - h.purAmt).toString(),
                prftRt = if (h.purAmt > 0) String.format("%.2f", (eval - h.purAmt).toDouble() / h.purAmt * 100) else "0",
                rmndQty = h.quantity.toString(), possRt = "0", predClosePric = h.basePric)
        }
        return HoldingListRes(totPurAmt = "0", totEvltAmt = items.sumOf { it.evltAmt.toLongOrNull() ?: 0L }.toString(), totEvltPl = "0", totPrftRt = "0", balance = res.balance.toString(), holdingList = items)
    }

    private fun fetchRealizedProfit(memberId: Long): RealizedFact? {
        val today = LocalDate.now()
        val rows = memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearAndMonthOrderByYearDescMonthDesc(memberId, MarketType.STOCK, today.year, today.monthValue)
        if (rows.isEmpty()) return null
        val names = brokerRepository.findAllByOrderByIdAsc().associate { it.id to it.name }
        return RealizedFact(
            monthTotalWon = rows.sumOf { it.realizedPnl },
            byBroker = rows.map { (names[it.broker.id] ?: "증권사") to it.realizedPnl },
        )
    }

    private fun <T> withMember(member: Member, block: () -> T): T {
        val previous = SecurityContextHolder.getContext().authentication
        val details = customUserDetailsService.loadUserByUsername(member.loginId)
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(details, null, details.authorities)
        return try {
            block()
        } finally {
            SecurityContextHolder.getContext().authentication = previous
        }
    }
}
