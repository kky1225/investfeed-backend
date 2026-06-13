package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 이동평균선 보정 모듈 (격상 전용).
 *
 * MA5 와 MA20 의 골든/데드크로스를 추천 방향과 일치할 때 한 단계 격상.
 * - BUY 추천 + MA5 > MA20 (골든크로스) → 매수 추세 확인 → 격상
 * - SELL 추천 + MA5 < MA20 (데드크로스) → 매도 추세 확인 → 격상
 *
 * 격하는 하지 않음 — 추세 전환 초기는 자연스러운 데드크로스 상태라 격하 부당.
 */
@Component
class MovingAverageModule : AdjustmentModule {
    override val name = "MovingAverage"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.movingAverageEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        val ma5 = pick.ma5 ?: return false
        val ma20 = pick.ma20 ?: return false
        return when (side) {
            Position.BUY -> ma5 > ma20   // 골든크로스
            Position.SELL -> ma5 < ma20  // 데드크로스
        }
    }

    // 방향은 shouldPromote 로만 표현(BUY=골든크로스 / SELL=데드크로스) — shouldDemote 미사용.
    override fun shouldDemote(pick: StockPick, side: Position): Boolean = false
}
