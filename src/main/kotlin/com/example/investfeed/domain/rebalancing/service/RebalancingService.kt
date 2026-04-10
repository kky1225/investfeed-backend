package com.example.investfeed.domain.rebalancing.service

import com.example.investfeed.domain.holding.service.AssetDashboardService
import com.example.investfeed.domain.rebalancing.dto.req.RebalancingSettingReq
import com.example.investfeed.domain.rebalancing.dto.res.*
import com.example.investfeed.domain.rebalancing.entity.RebalancingSetting
import com.example.investfeed.domain.rebalancing.repository.RebalancingSettingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RebalancingService(
    private val rebalancingSettingRepository: RebalancingSettingRepository,
    private val assetDashboardService: AssetDashboardService,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun saveSetting(req: RebalancingSettingReq): RebalancingSettingRes {
        val memberId = getMemberId()

        val existing = rebalancingSettingRepository.findByMemberId(memberId)
        val setting = if (existing != null) {
            existing.stockRatio = req.stockRatio
            existing.stockDirection = req.stockDirection
            existing.cryptoRatio = req.cryptoRatio
            existing.cryptoDirection = req.cryptoDirection
            existing.cashRatio = req.cashRatio
            existing.cashDirection = req.cashDirection
            existing.maxStockRatio = req.maxStockRatio
            existing.updatedAt = LocalDateTime.now()
            existing
        } else {
            rebalancingSettingRepository.save(
                RebalancingSetting(
                    memberId = memberId,
                    stockRatio = req.stockRatio,
                    stockDirection = req.stockDirection,
                    cryptoRatio = req.cryptoRatio,
                    cryptoDirection = req.cryptoDirection,
                    cashRatio = req.cashRatio,
                    cashDirection = req.cashDirection,
                    maxStockRatio = req.maxStockRatio,
                )
            )
        }

        return toSettingRes(setting)
    }

    fun getStatus(): RebalancingStatusRes? {
        val memberId = getMemberId()
        val setting = rebalancingSettingRepository.findByMemberId(memberId) ?: return null
        return calculateStatus(setting)
    }

    fun calculateStatus(setting: RebalancingSetting): RebalancingStatusRes {
        val dashboard = assetDashboardService.dashboard()
        val totalAsset = dashboard.totalAsset

        if (totalAsset <= 0) {
            return RebalancingStatusRes(
                setting = toSettingRes(setting),
                currentRatios = AssetRatioStatus(0.0, 0.0, 0.0, 0, 0, 0, 0),
                overweightAssets = emptyList(),
                overweightStocks = emptyList()
            )
        }

        val stockAmt = dashboard.stockSummary.evltAmt
        val cryptoAmt = dashboard.cryptoSummary.evltAmt
        val cashAmt = dashboard.totalCash

        val stockRatio = stockAmt.toDouble() / totalAsset * 100
        val cryptoRatio = cryptoAmt.toDouble() / totalAsset * 100
        val cashRatio = cashAmt.toDouble() / totalAsset * 100

        // 자산 유형 비중 초과 체크 (방향에 따라)
        val overweightAssets = mutableListOf<OverweightAssetItem>()

        checkAssetRatio("STOCK", stockRatio, setting.stockRatio, setting.stockDirection, totalAsset)?.let { overweightAssets.add(it) }
        checkAssetRatio("CRYPTO", cryptoRatio, setting.cryptoRatio, setting.cryptoDirection, totalAsset)?.let { overweightAssets.add(it) }
        checkAssetRatio("CASH", cashRatio, setting.cashRatio, setting.cashDirection, totalAsset)?.let { overweightAssets.add(it) }

        // 종목별 비중 초과 체크
        val overweightStocks = mutableListOf<OverweightStockItem>()
        val allHoldings = dashboard.stockSummary.holdings + dashboard.cryptoSummary.holdings

        for (holding in allHoldings) {
            val holdingRatio = holding.evltAmt.toDouble() / totalAsset * 100
            if (holdingRatio > setting.maxStockRatio) {
                val excessAmount = ((holdingRatio - setting.maxStockRatio) / 100 * totalAsset).toLong()
                val curPrc = holding.curPrc.replace(",", "").replace("+", "").replace("-", "").toLongOrNull() ?: 0
                val sellQuantity = if (curPrc > 0) excessAmount / curPrc else 0

                overweightStocks.add(
                    OverweightStockItem(
                        stkCd = holding.stkCd,
                        stkNm = holding.stkNm,
                        brokerName = holding.brokerName,
                        currentRatio = Math.round(holdingRatio * 10) / 10.0,
                        maxRatio = setting.maxStockRatio,
                        curPrc = curPrc,
                        evltAmt = holding.evltAmt,
                        sellQuantity = sellQuantity,
                        sellAmount = excessAmount
                    )
                )
            }
        }

        return RebalancingStatusRes(
            setting = toSettingRes(setting),
            currentRatios = AssetRatioStatus(
                stockRatio = Math.round(stockRatio * 10) / 10.0,
                cryptoRatio = Math.round(cryptoRatio * 10) / 10.0,
                cashRatio = Math.round(cashRatio * 10) / 10.0,
                stockAmount = stockAmt,
                cryptoAmount = cryptoAmt,
                cashAmount = cashAmt,
                totalAsset = totalAsset
            ),
            overweightAssets = overweightAssets,
            overweightStocks = overweightStocks
        )
    }

    private fun checkAssetRatio(assetType: String, currentRatio: Double, targetRatio: Int, direction: String, totalAsset: Long): OverweightAssetItem? {
        val isViolation = when (direction) {
            "MAX" -> currentRatio > targetRatio
            "MIN" -> currentRatio < targetRatio
            else -> false
        }
        if (!isViolation) return null

        val excessAmount = Math.abs((currentRatio - targetRatio) / 100 * totalAsset).toLong()
        return OverweightAssetItem(
            assetType = assetType,
            currentRatio = Math.round(currentRatio * 10) / 10.0,
            targetRatio = targetRatio,
            direction = direction,
            excessAmount = excessAmount
        )
    }

    @Transactional
    fun deleteSetting() {
        val memberId = getMemberId()
        val setting = rebalancingSettingRepository.findByMemberId(memberId)
            ?: throw IllegalArgumentException("설정이 존재하지 않습니다.")
        rebalancingSettingRepository.delete(setting)
    }

    private fun toSettingRes(setting: RebalancingSetting): RebalancingSettingRes {
        return RebalancingSettingRes(
            stockRatio = setting.stockRatio,
            stockDirection = setting.stockDirection,
            cryptoRatio = setting.cryptoRatio,
            cryptoDirection = setting.cryptoDirection,
            cashRatio = setting.cashRatio,
            cashDirection = setting.cashDirection,
            maxStockRatio = setting.maxStockRatio
        )
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
