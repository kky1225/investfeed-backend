package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 신고저 돌파 모듈 — **모멘텀 확인 (격상 전용)**.
 *
 * 키움 공식 52주(250일) 신고가/신저가 근접·돌파를 거래량 동반과 함께 확인.
 * "신고가 뚫으면 천장 모름 / 신저가 뚫으면 바닥 모름" 의 매물대 소멸 모멘텀을 시스템화.
 * 학술 근거: 52-Week High Effect (George & Hwang 2004).
 *
 * **HighLow52w 와 구분** (둘 다 ka10001 52주 고저 수치를 읽되 발동 로직만 다름):
 *  - HighLow52w = 52주 **위치 판단** (저점 +15% 반등 / 고점 -15% 하락) — 바닥/천장권 진입
 *  - Breakout   = 52주 **모멘텀 확인** (신고가 95% / 신저가 5% + 거래량) — 추세 정점 돌파
 *  - 다수결 룰이므로 가격 위치상 동시 발동 안 돼도 각자 격상풀의 한 표로 유효 (별도 트랙 아님)
 *
 * **격상 룰** (격하 없음, `canDemote = false`):
 *
 * | 시그널 | BUY 진영 | SELL 진영 |
 * |---|---|---|
 * | 52주 신고가 95% 근접/돌파 + 거래량 강함 | **격상** | — |
 * | 52주 신저가 5% 이내/돌파 + 거래량 강함 | — | **격상** |
 *
 * 거래량 동반 조건([RvolThreshold])으로 천장/바닥 허매수·허매도 1차 거름.
 *
 * **데이터**: 52주 고저는 키움 ka10001 공식 대비율(`distFromHigh52w`/`distFromLow52w`)
 * 재사용 — 추가 API 0, 자가계산 없음. 미조회 시 null → 미발동 (정상 폴백).
 */
@Component
class BreakoutModule : AdjustmentModule {
    override val name = "Breakout"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.breakoutEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        if (!isStrongVolume(pick)) return false
        return when (side) {
            // BUY 격상: 52주 신고가 95% 근접/돌파 (고점 대비 -5% 이상)
            Position.BUY -> (pick.distFromHigh52w ?: return false) >= -5.0
            // SELL 격상: 52주 신저가 5% 이내/돌파 (저점 대비 +5% 이하)
            Position.SELL -> (pick.distFromLow52w ?: return false) <= 5.0
        }
    }

    override fun shouldDemote(pick: StockPick, side: Position): Boolean = false

    // 격하 시그널 없음 — 모멘텀 확인 전용 모듈.
    override fun canDemote(side: Position): Boolean = false

    /**
     * 당일 거래량이 20일 평균의 [RvolThreshold] 배 이상인지 (VolumePriceModule 과 공유 임계).
     * 데이터 부족 시 false (무보정).
     */
    private fun isStrongVolume(pick: StockPick): Boolean {
        val today = pick.todayVolume ?: return false
        val avg = pick.avg20dVolume ?: return false
        if (avg <= 0L) return false
        return today.toDouble() / avg >= RvolThreshold.VALUE
    }
}
