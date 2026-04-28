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
        val brokerSummaries = mutableListOf<BrokerSummaryItem>()

        for (broker in stockBrokers) {
            var bEvltAmt = 0L
            var bPurAmt = 0L
            var bCash = 0L
            var bHoldingCount = 0
            val bHoldings = mutableListOf<BrokerHoldingItem>()

            if (broker.broker.type == BrokerType.API) {
                val res = holdingService.listHoldings()
                bEvltAmt = res.totEvltAmt.toLongOrNull() ?: 0
                bPurAmt = res.totPurAmt.toLongOrNull() ?: 0
                bCash = res.balance.toLongOrNull() ?: 0
                bHoldingCount = res.holdingList.size
                stockEvltAmt += bEvltAmt
                stockPurAmt += bPurAmt
                stockCash += bCash
                stockHoldings.addAll(res.holdingList.map { item ->
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
                stockCash += bCash
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
                holdingCount = bHoldingCount,
                holdings = bHoldings,
            ))
        }

        recalcPossRt(stockHoldings, stockEvltAmt)
        recalcPossRt(cryptoHoldings, cryptoEvltAmt)

        val totalEvltAmt = stockEvltAmt + cryptoEvltAmt
        val totalPurAmt = stockPurAmt + cryptoPurAmt
        val totalCash = stockCash + cryptoCash
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
            stockSummary = AssetGroupSummary(
                evltAmt = stockEvltAmt,
                purAmt = stockPurAmt,
                evltPl = stockEvltPl,
                prftRt = if (stockPurAmt > 0) String.format("%.2f", stockEvltPl.toDouble() / stockPurAmt * 100) else "0",
                cash = stockCash,
                ratio = stockRatio,
                holdings = stockHoldings,
            ),
            cryptoSummary = AssetGroupSummary(
                evltAmt = cryptoEvltAmt,
                purAmt = cryptoPurAmt,
                evltPl = cryptoEvltPl,
                prftRt = if (cryptoPurAmt > 0) String.format("%.2f", cryptoEvltPl.toDouble() / cryptoPurAmt * 100) else "0",
                cash = cryptoCash,
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
