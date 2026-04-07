package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.res.*
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
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
    private val log = KotlinLogging.logger {}

    fun dashboard(): AssetDashboardRes {
        val memberId = getMemberId()
        val allBrokers = memberBrokerRepository.findByMemberIdOrderByOrderIndex(memberId)

        val stockBrokers = allBrokers.filter { it.broker.market == MarketType.STOCK }
        val cryptoBrokers = allBrokers.filter { it.broker.market == MarketType.CRYPTO }

        // 주식 자산 수집
        val stockHoldings = mutableListOf<UnifiedHoldingItem>()
        var stockEvltAmt = 0L
        var stockPurAmt = 0L
        var stockCash = 0L

        for (broker in stockBrokers) {
            if (broker.broker.type == BrokerType.API) {
                try {
                    val res = holdingService.holdingList()
                    stockEvltAmt += res.totEvltAmt.toLongOrNull() ?: 0
                    stockPurAmt += res.totPurAmt.toLongOrNull() ?: 0
                    stockCash += res.balance.toLongOrNull() ?: 0
                    stockHoldings.addAll(res.holdingList.map { item ->
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
                } catch (e: Exception) {
                    log.error { "주식 API 보유자산 조회 실패 (${broker.broker.name}): ${e.message}" }
                }
            } else {
                try {
                    val res = manualHoldingService.manualHoldingList(broker.id)
                    stockCash += res.balance
                    for (item in res.holdings) {
                        val evltAmt = (item.curPrc.toLongOrNull() ?: 0) * item.quantity
                        val evltPl = evltAmt - item.purAmt
                        val prftRt = if (item.purAmt > 0) evltPl.toDouble() / item.purAmt * 100 else 0.0
                        stockEvltAmt += evltAmt
                        stockPurAmt += item.purAmt
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
                } catch (e: Exception) {
                    log.error { "주식 수동 보유자산 조회 실패 (${broker.broker.name}): ${e.message}" }
                }
            }
        }

        // 코인 자산 수집
        val cryptoHoldings = mutableListOf<UnifiedHoldingItem>()
        var cryptoEvltAmt = 0L
        var cryptoPurAmt = 0L
        var cryptoCash = 0L

        for (broker in cryptoBrokers) {
            if (broker.broker.type == BrokerType.API) {
                try {
                    val res = cryptoHoldingService.cryptoHoldingList()
                    cryptoEvltAmt += res.totEvltAmt.toDoubleOrNull()?.toLong() ?: 0
                    cryptoPurAmt += res.totPurAmt.toDoubleOrNull()?.toLong() ?: 0
                    cryptoCash += res.balance.toDoubleOrNull()?.toLong() ?: 0
                    cryptoHoldings.addAll(res.holdingList.map { item ->
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
                } catch (e: Exception) {
                    log.error { "코인 API 보유자산 조회 실패 (${broker.broker.name}): ${e.message}" }
                }
            } else {
                try {
                    val res = cryptoManualHoldingService.manualHoldingList(broker.id)
                    cryptoCash += res.balance
                    for (item in res.holdings) {
                        val evltAmt = (item.curPrc.toLongOrNull() ?: 0) * item.quantity
                        val evltPl = evltAmt - item.purAmt
                        val prftRt = if (item.purAmt > 0) evltPl.toDouble() / item.purAmt * 100 else 0.0
                        cryptoEvltAmt += evltAmt
                        cryptoPurAmt += item.purAmt
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
                } catch (e: Exception) {
                    log.error { "코인 수동 보유자산 조회 실패 (${broker.broker.name}): ${e.message}" }
                }
            }
        }

        // 비중(possRt) 재계산
        recalcPossRt(stockHoldings, stockEvltAmt)
        recalcPossRt(cryptoHoldings, cryptoEvltAmt)

        // 전체 합산
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
