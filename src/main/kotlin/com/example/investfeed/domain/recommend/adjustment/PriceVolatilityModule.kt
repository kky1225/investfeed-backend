package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 가격 변동성 보정 모듈 (격하 전용).
 *
 * 5일 누적 등락률이 추천과 반대 방향으로 ±10% 이상 변동했을 때 한 단계 격하 권유.
 * - BUY 추천 + flu5Pct ≤ -10% → 단기 급락 — 추세 신뢰 약화 → 격하
 * - SELL 추천 + flu5Pct ≥ +10% → 단기 급반등 — 추세 신뢰 약화 → 격하
 *
 * 격상은 하지 않음 (변동성 = 위험 시그널이라 격상 부적합).
 */
@Component
class PriceVolatilityModule : AdjustmentModule {
    override val name = "PriceVolatility"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.priceVolatilityEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean = false

    override fun shouldDemote(pick: StockPick, side: Position): Boolean {
        val flu5 = pick.flu5Pct ?: return false
        return when (side) {
            Position.BUY -> flu5 <= -10.0
            Position.SELL -> flu5 >= 10.0
        }
    }

    /** 격하 전용 모듈 — 격상 능력 없음. 만장일치 격상 판정에서 제외. */
    override fun canPromote(side: Position): Boolean = false
}
