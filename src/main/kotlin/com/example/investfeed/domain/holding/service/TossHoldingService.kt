package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.auth.exception.ApiKeyNotFoundException
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.toss.account.client.TossAccountClient
import com.example.investfeed.toss.exception.TossBuyingPowerException
import com.example.investfeed.toss.exception.TossExchangeRateException
import com.example.investfeed.toss.exchangerate.client.TossExchangeRateClient
import com.example.investfeed.toss.holding.TossSymbolMapper
import com.example.investfeed.toss.holding.client.TossBuyingPowerClient
import com.example.investfeed.toss.holding.client.TossHoldingClient
import com.example.investfeed.toss.holding.dto.res.TossHoldingItem
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class TossHoldingService(
    private val tossAccountClient: TossAccountClient,
    private val tossHoldingClient: TossHoldingClient,
    private val tossExchangeRateClient: TossExchangeRateClient,
    private val tossBuyingPowerClient: TossBuyingPowerClient,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val usStockMasterRepository: UsStockMasterRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val BROKER_NAME = "토스증권"
        private const val CURRENCY_USD = "USD"
        private const val CURRENCY_KRW = "KRW"
    }

    private data class Valuation(val curPrc: Long, val purPric: Long, val evltAmt: Long, val purAmt: Long, val dayPl: Long, val evltPl: Long, val prftRt: Double)

    fun listTossHoldings(): HoldingListRes {
        val loginId = getLoginId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val memberId = getMemberId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")

        val tossBroker = brokerRepository.findByName(BROKER_NAME)
            ?: throw IllegalArgumentException("토스증권을 찾을 수 없습니다.")

        memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, tossBroker.id)
            ?: throw ApiKeyNotFoundException()

        val accountSeq = resolveAccountSeq()

        val balance = accountSeq?.let { fetchKrwBuyingPower(it) } ?: 0L
        val balanceUsd = accountSeq?.let { fetchUsdBuyingPower(it) }
        val items = accountSeq?.let { tossHoldingClient.getHoldings(it)?.items } ?: emptyList()

        val needsFx = items.any { TossSymbolMapper.isUs(it.marketCountry) } || (balanceUsd?.toDoubleOrNull() ?: 0.0) > 0.0
        val usdKrwRate = if (needsFx) fetchUsdKrwRate() else 0.0
        val balanceUsdKrw = balanceUsd?.toDoubleOrNull()?.let { (it * usdKrwRate).toLong().toString() }

        if (items.isEmpty()) {
            memberHoldingSyncService.sync(memberId, emptyList(), tossBroker)
            return HoldingListRes(
                totPurAmt = "0",
                totEvltAmt = "0",
                totEvltPl = "0",
                totPrftRt = "0",
                balance = balance.toString(),
                balanceUsd = balanceUsd,
                balanceUsdKrw = balanceUsdKrw,
                holdingList = emptyList()
            )
        }

        val stexTpBySymbol = resolveStexTps(items)

        var totEvltAmt = 0L
        var totPurAmt = 0L
        var totEvltPl = 0L

        val holdingList = items.mapNotNull { item ->
            val symbol = item.symbol ?: return@mapNotNull null
            val stkCd = TossSymbolMapper.toStkCd(symbol, item.marketCountry)
            val isUs = TossSymbolMapper.isUs(item.marketCountry)
            val v = evaluate(item, usdKrwRate)

            totEvltAmt += v.evltAmt
            totPurAmt += v.purAmt
            totEvltPl += v.evltPl

            HoldingItem(
                stkCd = stkCd,
                stkNm = item.name ?: symbol,
                curPrc = v.curPrc.toString(),
                purPric = v.purPric.toString(),
                purAmt = v.purAmt.toString(),
                evltAmt = v.evltAmt.toString(),
                evltvPrft = v.evltPl.toString(),
                prftRt = String.format("%.2f", v.prftRt),
                rmndQty = item.quantity ?: "0",
                possRt = "0",
                predClosePric = "0",
                dayPl = v.dayPl.toString(),
                stexTp = if (isUs) stexTpBySymbol[symbol] else null,
                usStkCd = if (isUs) symbol else null,
                curPrcUsd = if (isUs) item.lastPrice else null,
                purPricUsd = if (isUs) item.averagePurchasePrice else null,
                evltAmtUsd = if (isUs) item.marketValue?.amount else null,
                evltvPrftUsd = if (isUs) item.profitLoss?.amount else null,
            )
        }

        val holdingListWithPossRt = holdingList.map { hi ->
            val evlt = hi.evltAmt.toLongOrNull() ?: 0L
            val possRt = if (totEvltAmt > 0) evlt.toDouble() / totEvltAmt * 100 else 0.0
            hi.copy(possRt = String.format("%.2f", possRt))
        }

        val totPrftRt = if (totPurAmt > 0) totEvltPl.toDouble() / totPurAmt * 100 else 0.0

        memberHoldingSyncService.sync(
            memberId = memberId,
            holdings = holdingListWithPossRt.map { it.stkCd to it.stkNm },
            broker = tossBroker
        )

        val memberHoldings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, tossBroker.id)
        val holdingMap = holdingListWithPossRt.associateBy { it.stkCd }
        val sortedHoldingList = memberHoldings.mapNotNull { mh ->
            holdingMap[mh.stkCd]?.copy(id = mh.id)
        }

        return HoldingListRes(
            totPurAmt = totPurAmt.toString(),
            totEvltAmt = totEvltAmt.toString(),
            totEvltPl = totEvltPl.toString(),
            totPrftRt = String.format("%.2f", totPrftRt),
            balance = balance.toString(),
            balanceUsd = balanceUsd,
            balanceUsdKrw = balanceUsdKrw,
            holdingList = sortedHoldingList
        )
    }

    private fun resolveStexTps(items: List<TossHoldingItem>): Map<String, String> {
        val usSymbols = items.filter { TossSymbolMapper.isUs(it.marketCountry) }
            .mapNotNull { it.symbol }
            .distinct()
        if (usSymbols.isEmpty()) return emptyMap()

        val stexTpBySymbol = usStockMasterRepository.findByStkCdIn(usSymbols)
            .associate { it.stkCd to it.stexTp }

        val missing = usSymbols - stexTpBySymbol.keys
        if (missing.isNotEmpty()) {
            log.warn { "미국 종목 마스터 미등록으로 거래소구분을 채우지 못했습니다: $missing" }
        }
        return stexTpBySymbol
    }

    private fun evaluate(
        item: TossHoldingItem,
        usdKrwRate: Double,
    ): Valuation {
        val itemPurAmt = item.marketValue?.purchaseAmount?.toDoubleOrNull() ?: 0.0
        val itemEvltAmt = item.marketValue?.amount?.toDoubleOrNull() ?: 0.0
        val avgPrice = item.averagePurchasePrice?.toDoubleOrNull() ?: 0.0
        val lastPrice = item.lastPrice?.toDoubleOrNull() ?: 0.0
        val itemDayPl = item.dailyProfitLoss?.amount?.toDoubleOrNull() ?: 0.0
        val itemEvltPl = item.profitLoss?.amount?.toDoubleOrNull() ?: 0.0
        val itemPrftRt = (item.profitLoss?.rate?.toDoubleOrNull() ?: 0.0) * 100

        val rate = if (TossSymbolMapper.isUs(item.marketCountry)) usdKrwRate else 1.0
        return Valuation(
            curPrc = (lastPrice * rate).toLong(),
            purPric = (avgPrice * rate).toLong(),
            evltAmt = (itemEvltAmt * rate).toLong(),
            purAmt = (itemPurAmt * rate).toLong(),
            dayPl = (itemDayPl * rate).toLong(),
            evltPl = (itemEvltPl * rate).toLong(),
            prftRt = itemPrftRt,
        )
    }

    private fun resolveAccountSeq(): Long? {
        val accounts = tossAccountClient.getAccounts()
        return (accounts.firstOrNull { it.accountType == "BROKERAGE" } ?: accounts.firstOrNull())?.accountSeq
    }

    private fun fetchKrwBuyingPower(accountSeq: Long): Long {
        val amount = tossBuyingPowerClient.getBuyingPower(accountSeq, CURRENCY_KRW)?.result?.cashBuyingPower
        val parsed = amount?.toDoubleOrNull()
        if (parsed == null) {
            log.warn { "토스 원화 주문가능금액 조회 실패 — 현금 금액을 확정할 수 없어 조회를 중단합니다." }
            throw TossBuyingPowerException()
        }
        return parsed.toLong()
    }

    private fun fetchUsdBuyingPower(accountSeq: Long): String? {
        return runCatching {
            tossBuyingPowerClient.getBuyingPower(accountSeq, CURRENCY_USD)?.result?.cashBuyingPower
        }.onFailure {
            log.warn { "토스 달러 주문가능금액 조회 실패 — 표시를 생략합니다: ${it.message}" }
        }.getOrNull()
    }

    private fun fetchUsdKrwRate(): Double {
        val result = tossExchangeRateClient.getRate("USD", "KRW")?.result
        val rate = result?.midRate?.toDoubleOrNull() ?: result?.rate?.toDoubleOrNull()
        if (rate == null || rate <= 0.0) {
            log.warn { "토스 USD/KRW 환율 조회 실패 — 원화 환산 불가로 보유종목 조회를 중단합니다." }
            throw TossExchangeRateException()
        }
        return rate
    }

    private fun getLoginId(): String? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.username
    }

    private fun getMemberId(): Long? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id
    }
}
