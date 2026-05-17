package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * RSI 보정 모듈 (추세 추종 관점).
 *
 * 우리 시스템 = Smart Money Follower (외인+연기금 추종) = 추세 추종 영역.
 * 평균회귀 30/70 임계 적용 시 시점 미스매치 발생 → **50선 기준 + 70 이탈 N일 확정** 룰 사용.
 *
 * **보정 룰**:
 *
 * | 조건 | BUY 진영 | SELL 진영 |
 * |---|---|---|
 * | `RSI < 50` | **격상** (저점 매수 메리트) | **격하** (가격 약세 → 매도 위험) |
 * | `RSI ≥ 70 → 3일 < 70 유지` (Breakdown70) | **격하** (모멘텀 약화 확정) | **격상** (매도 기회 강화) |
 *
 * **Breakdown70 정의**:
 * - 3일 전 RSI ≥ 70 (3일 전엔 과매수)
 * - AND 최근 3일 RSI 모두 < 70 (회복 못 함)
 * - → "70 도달 후 3일 회복 못 함 = 진짜 모멘텀 약화"
 */
@Component
class RsiModule : AdjustmentModule {
    override val name = "Rsi"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.rsiEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        return when (side) {
            // BUY 격상: RSI < 50 (저점 매수 메리트)
            Position.BUY -> (pick.rsi14 ?: return false) < 50.0
            // SELL 격상: 70 이탈 3일 (모멘텀 약화 = 매도 기회 강화)
            Position.SELL -> pick.rsi14Breakdown70 == true
        }
    }

    override fun shouldDemote(pick: StockPick, side: Position): Boolean {
        return when (side) {
            // BUY 격하: 70 이탈 3일 (모멘텀 약화)
            Position.BUY -> pick.rsi14Breakdown70 == true
            // SELL 격하: RSI < 50 (가격 약세 → 매도 위험)
            Position.SELL -> (pick.rsi14 ?: return false) < 50.0
        }
    }
}
