package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 이동평균선 보정 모듈 (격상 전용) — **사건화(event-based)**.
 *
 * MA5/MA20 골든/데드크로스가 추천 방향과 일치할 때 한 단계 격상하되,
 * 교차 **발생 후 [EVENT_WINDOW_DAYS]일 이내(신선한 교차)만** 표가 유효하다.
 * - BUY 추천 + 골든크로스 발생 ≤ N일 → 매수 추세 전환 확인 → 격상
 * - SELL 추천 + 데드크로스 발생 ≤ N일 → 매도 추세 전환 확인 → 격상
 *
 * 교차는 사건(타이밍) 신호라 정보가 발생일 근처에만 있다. 상태(`ma5 > ma20`)로만 보면
 * 국면 증폭기로 변질된다 — 상승장 매수표 93%, 전환기의 낡은 골든크로스는 역신호
 * (매도 픽 잔존 매수표 그룹 +20일 −19.2% vs 없음 −7.5%), 바닥의 낡은 데드크로스는
 * 수급 반전 진입을 방해(+7.3% 놓침). 신선한 데드크로스는 하락 초입 방어로 유효(−4.9% vs −1.6%).
 * (2026-08-24 사건화 전환. N=5 는 ma5 창 = 교차 정보의 수명 근거 시작값 — 표본 축적 후 3/10 과 비교)
 *
 * 격하는 하지 않음 — 방향은 shouldPromote 로만 표현(BUY=골든크로스 / SELL=데드크로스).
 */
@Component
class MovingAverageModule : AdjustmentModule {
    override val name = "MovingAverage"

    companion object {
        /** 교차 신선도 창(거래일). 교차 당일 = 1일. */
        const val EVENT_WINDOW_DAYS = 5
    }

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.movingAverageEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        val ma5 = pick.ma5 ?: return false
        val ma20 = pick.ma20 ?: return false
        val age = pick.maCrossAge ?: return false
        if (age > EVENT_WINDOW_DAYS) return false
        return when (side) {
            Position.BUY -> ma5 > ma20   // 골든크로스
            Position.SELL -> ma5 < ma20  // 데드크로스
        }
    }

    // 방향은 shouldPromote 로만 표현(BUY=골든크로스 / SELL=데드크로스) — shouldDemote 미사용.
    override fun shouldDemote(pick: StockPick, side: Position): Boolean = false
}
