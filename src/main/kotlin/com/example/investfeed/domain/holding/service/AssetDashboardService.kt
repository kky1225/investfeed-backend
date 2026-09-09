package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.res.*
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AssetDashboardService(
    private val memberBrokerRepository: MemberBrokerRepository,
    private val holdingService: HoldingService,
    private val tossHoldingService: TossHoldingService,
    private val cryptoHoldingService: CryptoHoldingService,
    private val manualHoldingService: ManualHoldingService,
    private val cryptoManualHoldingService: CryptoManualHoldingService,
) {

    fun getAssetDashboard(): AssetDashboardRes {
        val memberId = getMemberId()
        val allBrokers = memberBrokerRepository.findByMemberIdOrderByOrderIndex(memberId)

        val stockBrokers = allBrokers.filter { it.broker.market == MarketType.STOCK }
        val cryptoBrokers = allBrokers.filter { it.broker.market == MarketType.CRYPTO }

        val stockHoldings = mutableListOf<UnifiedHoldingItem>()
        var stockEvltAmt = 0L
        var stockPurAmt = 0L
        var stockCash = 0L
        var stockCashKrw = 0L
        var stockCashUsd = 0.0
        var stockHasUsd = false
        val brokerSummaries = mutableListOf<BrokerSummaryItem>()

        for (broker in stockBrokers) {
            var bEvltAmt = 0L
            var bPurAmt = 0L
            var bCash = 0L
            var bCashKrw = 0L
            var bCashUsd = 0.0
            var bHasUsd = false
            var bHoldingCount = 0
            val bHoldings = mutableListOf<BrokerHoldingItem>()

            if (broker.broker.type == BrokerType.API) {
                val responses = when (broker.broker.name) {
                    "토스증권" -> listOf(tossHoldingService.listTossHoldings())
                    else -> listOf(holdingService.listHoldings())
                }

                bEvltAmt = responses.sumOf { it.totEvltAmt.toLongOrNull() ?: 0 }
                bPurAmt = responses.sumOf { it.totPurAmt.toLongOrNull() ?: 0 }
                bCashKrw = responses.sumOf { it.balance.toLongOrNull() ?: 0 }
                bCashUsd = responses.sumOf { it.balanceUsd?.toDoubleOrNull() ?: 0.0 }
                bHasUsd = responses.any { it.balanceUsd != null }
                bCash = bCashKrw + responses.sumOf { it.balanceUsdKrw?.toLongOrNull() ?: 0 }
                stockCashKrw += bCashKrw
                stockCashUsd += bCashUsd
                if (bHasUsd) stockHasUsd = true
                val allHoldings = responses.flatMap { it.holdingList }
                bHoldingCount = allHoldings.size
                stockEvltAmt += bEvltAmt
                stockPurAmt += bPurAmt
                stockCash += bCash
                stockHoldings.addAll(allHoldings.map { item ->
                    bHoldings.add(BrokerHoldingItem(
                        stkCd = item.stkCd,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt.toLongOrNull() ?: 0,
                        quantity = item.rmndQty.toDoubleOrNull() ?: 0.0,
                    ))
                    UnifiedHoldingItem(
                        stkCd = item.stkCd,
                        stkNm = item.stkNm,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt.toLongOrNull() ?: 0,
                        evltAmt = item.evltAmt.toLongOrNull() ?: 0,
                        evltPl = item.evltvPrft.toLongOrNull() ?: 0,
                        prftRt = item.prftRt,
                        possRt = "0",
                        brokerName = broker.broker.name,
                    )
                })
            } else {
                val res = manualHoldingService.listManualHoldings(broker.id)
                bCash = res.balance
                bCashKrw = res.balance // 수동 계좌는 원화만 입력받는다
                stockCash += bCash
                stockCashKrw += bCash
                bHoldingCount = res.holdings.size
                for (item in res.holdings) {
                    val evltAmt = (item.curPrc.toLongOrNull() ?: 0) * item.quantity
                    val evltPl = evltAmt - item.purAmt
                    val prftRt = if (item.purAmt > 0) evltPl.toDouble() / item.purAmt * 100 else 0.0
                    bEvltAmt += evltAmt
                    bPurAmt += item.purAmt
                    stockEvltAmt += evltAmt
                    stockPurAmt += item.purAmt
                    bHoldings.add(BrokerHoldingItem(
                        stkCd = item.stkCd,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt,
                        quantity = item.quantity.toDouble(),
                    ))
                    stockHoldings.add(UnifiedHoldingItem(
                        stkCd = item.stkCd,
                        stkNm = item.stkNm,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt,
                        evltAmt = evltAmt,
                        evltPl = evltPl,
                        prftRt = String.format("%.2f", prftRt),
                        possRt = "0",
                        brokerName = broker.broker.name,
                    ))
                }
            }

            val bEvltPl = bEvltAmt - bPurAmt
            brokerSummaries.add(BrokerSummaryItem(
                brokerName = broker.broker.name,
                market = broker.broker.market.name,
                type = broker.broker.type.name,
                evltAmt = bEvltAmt,
                purAmt = bPurAmt,
                evltPl = bEvltPl,
                prftRt = if (bPurAmt > 0) String.format("%.2f", bEvltPl.toDouble() / bPurAmt * 100) else "0",
                cash = bCash,
                cashKrw = bCashKrw,
                cashUsd = if (bHasUsd) String.format("%.2f", bCashUsd) else null,
                holdingCount = bHoldingCount,
                holdings = bHoldings,
            ))
        }

        val cryptoHoldings = mutableListOf<UnifiedHoldingItem>()
        var cryptoEvltAmt = 0L
        var cryptoPurAmt = 0L
        var cryptoCash = 0L

        for (broker in cryptoBrokers) {
            var bEvltAmt = 0L
            var bPurAmt = 0L
            var bCash = 0L
            var bHoldingCount = 0
            val bHoldings = mutableListOf<BrokerHoldingItem>()

            if (broker.broker.type == BrokerType.API) {
                val res = cryptoHoldingService.listCryptoHoldings()
                bEvltAmt = res.totEvltAmt.toDoubleOrNull()?.toLong() ?: 0
                bPurAmt = res.totPurAmt.toDoubleOrNull()?.toLong() ?: 0
                bCash = res.balance.toDoubleOrNull()?.toLong() ?: 0
                bHoldingCount = res.holdingList.size
                cryptoEvltAmt += bEvltAmt
                cryptoPurAmt += bPurAmt
                cryptoCash += bCash
                cryptoHoldings.addAll(res.holdingList.map { item ->
                    bHoldings.add(BrokerHoldingItem(
                        stkCd = item.stkCd,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt.toLongOrNull() ?: 0,
                        quantity = item.rmndQty.toDoubleOrNull() ?: 0.0,
                    ))
                    UnifiedHoldingItem(
                        stkCd = item.stkCd,
                        stkNm = item.stkNm,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt.toLongOrNull() ?: 0,
                        evltAmt = item.evltAmt.toLongOrNull() ?: 0,
                        evltPl = item.evltvPrft.toLongOrNull() ?: 0,
                        prftRt = item.prftRt,
                        possRt = "0",
                        brokerName = broker.broker.name,
                    )
                })
            } else {
                val res = cryptoManualHoldingService.listCryptoManualHoldings(broker.id)
                bCash = res.balance
                cryptoCash += bCash
                bHoldingCount = res.holdings.size
                for (item in res.holdings) {
                    val evltAmt = (item.curPrc.toLongOrNull() ?: 0) * item.quantity
                    val evltPl = evltAmt - item.purAmt
                    val prftRt = if (item.purAmt > 0) evltPl.toDouble() / item.purAmt * 100 else 0.0
                    bEvltAmt += evltAmt
                    bPurAmt += item.purAmt
                    cryptoEvltAmt += evltAmt
                    cryptoPurAmt += item.purAmt
                    bHoldings.add(BrokerHoldingItem(
                        stkCd = item.stkCd,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt,
                        quantity = item.quantity.toDouble(),
                    ))
                    cryptoHoldings.add(UnifiedHoldingItem(
                        stkCd = item.stkCd,
                        stkNm = item.stkNm,
                        curPrc = item.curPrc,
                        purAmt = item.purAmt,
                        evltAmt = evltAmt,
                        evltPl = evltPl,
                        prftRt = String.format("%.2f", prftRt),
                        possRt = "0",
                        brokerName = broker.broker.name,
                    ))
                }
            }

            val bEvltPl = bEvltAmt - bPurAmt
            brokerSummaries.add(BrokerSummaryItem(
                brokerName = broker.broker.name,
                market = broker.broker.market.name,
                type = broker.broker.type.name,
                evltAmt = bEvltAmt,
                purAmt = bPurAmt,
                evltPl = bEvltPl,
                prftRt = if (bPurAmt > 0) String.format("%.2f", bEvltPl.toDouble() / bPurAmt * 100) else "0",
                cash = bCash,
                cashKrw = bCash,
                holdingCount = bHoldingCount,
                holdings = bHoldings,
            ))
        }

        recalcPossRt(stockHoldings, stockEvltAmt)
        recalcPossRt(cryptoHoldings, cryptoEvltAmt)

        val totalEvltAmt = stockEvltAmt + cryptoEvltAmt
        val totalPurAmt = stockPurAmt + cryptoPurAmt
        val totalCash = stockCash + cryptoCash
        val usdCashLabel = if (stockHasUsd) String.format("%.2f", stockCashUsd) else null
        val totalEvltPl = totalEvltAmt - totalPurAmt
        val totalPrftRt = if (totalPurAmt > 0) String.format("%.2f", totalEvltPl.toDouble() / totalPurAmt * 100) else "0"
        val totalAsset = totalEvltAmt + totalCash

        val stockTotal = stockEvltAmt + stockCash
        val cryptoTotal = cryptoEvltAmt + cryptoCash
        val stockRatio = if (totalAsset > 0) String.format("%.1f", stockTotal.toDouble() / totalAsset * 100) else "0"
        val cryptoRatio = if (totalAsset > 0) String.format("%.1f", cryptoTotal.toDouble() / totalAsset * 100) else "0"

        val stockEvltPl = stockEvltAmt - stockPurAmt
        val cryptoEvltPl = cryptoEvltAmt - cryptoPurAmt

        return AssetDashboardRes(
            totalAsset = totalAsset,
            totalEvltAmt = totalEvltAmt,
            totalPurAmt = totalPurAmt,
            totalEvltPl = totalEvltPl,
            totalPrftRt = totalPrftRt,
            totalCash = totalCash,
            totalCashKrw = stockCashKrw + cryptoCash,
            totalCashUsd = usdCashLabel,
            stockSummary = AssetGroupSummary(
                evltAmt = stockEvltAmt,
                purAmt = stockPurAmt,
                evltPl = stockEvltPl,
                prftRt = if (stockPurAmt > 0) String.format("%.2f", stockEvltPl.toDouble() / stockPurAmt * 100) else "0",
                cash = stockCash,
                cashKrw = stockCashKrw,
                cashUsd = usdCashLabel,
                ratio = stockRatio,
                holdings = stockHoldings,
            ),
            cryptoSummary = AssetGroupSummary(
                evltAmt = cryptoEvltAmt,
                purAmt = cryptoPurAmt,
                evltPl = cryptoEvltPl,
                prftRt = if (cryptoPurAmt > 0) String.format("%.2f", cryptoEvltPl.toDouble() / cryptoPurAmt * 100) else "0",
                cash = cryptoCash,
                cashKrw = cryptoCash, // 코인 계좌는 원화만
                ratio = cryptoRatio,
                holdings = cryptoHoldings,
            ),
            brokerSummaries = brokerSummaries,
        )
    }

    private fun recalcPossRt(holdings: MutableList<UnifiedHoldingItem>, totalEvltAmt: Long) {
        if (totalEvltAmt <= 0) return
        for (i in holdings.indices) {
            val ratio = holdings[i].evltAmt.toDouble() / totalEvltAmt * 100
            holdings[i] = holdings[i].copy(possRt = String.format("%.2f", ratio))
        }
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
