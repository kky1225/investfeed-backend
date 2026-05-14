package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.domain.recommend.entity.StockPick
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 코스피/코스닥 매크로 보정 모듈.
 *
 * 사용자 추천 조회 시점에 [adjust] 호출 → Redis 캐시에서 매크로 스냅샷 조회 →
 * 6가지 케이스에 따라 추천 등급을 한 단계 격상/격하/유지.
 *
 * **6가지 케이스 (단순 부호 기반)**
 *
 * | 지수 | 기관 | 외국인 | 동작 (BUY 진영) | 동작 (SELL 진영) |
 * |---|---|---|---|---|
 * | 상승 | 매수 | 매수 | **격상** (BUY → STRONG_BUY) | - |
 * | 상승 | 매수 | 매도 | 유지 | - |
 * | 상승 | 매도 | 매도 | **격하** (BUY → HOLD, STRONG_BUY → BUY) | - |
 * | 하락 | 매도 | 매도 | - | **격상** (SELL → STRONG_SELL) |
 * | 하락 | 매수 | 매도 | - | 유지 |
 * | 하락 | 매수 | 매수 | - | **격하** (SELL → HOLD, STRONG_SELL → SELL) |
 *
 * - HOLD 종목은 방향 정보 없으므로 보정 대상 아님
 * - 캐시 미스/marketType 없음 → 무보정 fallback (기존 추천 그대로)
 * - 분류 방향 절대 안 바뀜 (BUY ↔ SELL 불가)
 */
@Component
class MarketIndexAdjustmentModule(
    private val marketMacroCacheService: MarketMacroCacheService,
) {
    private val log = KotlinLogging.logger {}

    /**
     * 매크로 보정 적용. 사용자가 옵션 ON 한 상태에서만 호출.
     */
    fun adjust(currentType: String, pick: StockPick): String {
        // HOLD 는 방향 정보 없음 → 보정 대상 X
        if (currentType == "HOLD") return currentType
        val marketType = pick.marketType?.takeIf { it.isNotBlank() } ?: return currentType

        val snapshot = marketMacroCacheService.getSnapshot(marketType) ?: run {
            // 캐시 미스 (장 시작 전, 주말, polling 장애 등) → 무보정
            return currentType
        }

        val isUp = snapshot.priceChangeRate.signum() > 0
        val isDown = snapshot.priceChangeRate.signum() < 0
        val instBuy = snapshot.institutionalNetBuy > 0
        val instSell = snapshot.institutionalNetBuy < 0
        val foreignBuy = snapshot.foreignNetBuy > 0
        val foreignSell = snapshot.foreignNetBuy < 0
        val isBuySide = currentType == "STRONG_BUY" || currentType == "BUY"
        val isSellSide = currentType == "STRONG_SELL" || currentType == "SELL"

        return when {
            // 케이스 1: 상승 + 기관/외국인 매수 + BUY 진영 → 격상
            isUp && instBuy && foreignBuy && isBuySide -> promoteOnce(currentType)
            // 케이스 3: 상승 + 기관/외국인 매도 + BUY 진영 → 격하
            isUp && instSell && foreignSell && isBuySide -> demoteOnce(currentType)
            // 케이스 4: 하락 + 기관/외국인 매도 + SELL 진영 → 격상
            isDown && instSell && foreignSell && isSellSide -> promoteOnce(currentType)
            // 케이스 6: 하락 + 기관/외국인 매수 + SELL 진영 → 격하
            isDown && instBuy && foreignBuy && isSellSide -> demoteOnce(currentType)
            // 그 외 (케이스 2, 5 + divergence + 등락 미미) → 유지
            else -> currentType
        }
    }

    private fun promoteOnce(type: String): String = when (type) {
        "BUY" -> "STRONG_BUY"
        "SELL" -> "STRONG_SELL"
        else -> type  // 이미 STRONG 이면 그대로
    }

    private fun demoteOnce(type: String): String = when (type) {
        "STRONG_BUY" -> "BUY"
        "BUY" -> "HOLD"
        "STRONG_SELL" -> "SELL"
        "SELL" -> "HOLD"
        else -> type
    }
}
