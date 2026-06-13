package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 52주 신고가/신저가 모듈 — **Stage Analysis (Stan Weinstein) 기반 진입 시그널**.
 *
 * 단순 신고가/신저가 = 노이즈 큼 → **반등/하락 폭 + 추세 확인** 으로 보수적 판별.
 *
 * **격상 룰**:
 *
 * | 시그널 | BUY 진영 | SELL 진영 |
 * |---|---|---|
 * | 저점 +15% 이상 + 종가 > MA20 (Stage 2 진입) | **격상** | — |
 * | 고점 -15% 이하 + 종가 < MA20 (Stage 4 진입) | — | **격상** |
 *
 * **격하(shouldDemote) 미사용** — 방향은 shouldPromote 로만 표현(매수쪽=저점반등 / 매도쪽=고점하락).
 *
 * **왜 MA20 vs flu5Pct**:
 *  - MA20 = 평활화된 추세 위치 (학술 표준)
 *  - flu5Pct = 노이즈 큼 (큰 변동 한 번에 부호 바뀜)
 *  - [MovingAverageModule] 골든/데드크로스 (이벤트) 와 종가 vs MA20 (상태) 는 중복 X
 *
 * **점수제**: 매수쪽/매도쪽 한 표 — 단독으로 등급을 못 바꾸고, 표 합산이 문턱(매수 net≥2 / 매도 net≥1) 넘을 때만 이동.
 */
@Component
class HighLow52wModule : AdjustmentModule {
    override val name = "HighLow52w"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.highLow52wEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        val close = pick.closeAboveMa20 ?: return false
        return when (side) {
            // BUY 격상 (Stage 2): 저점 +15% 이상 + 종가 > MA20
            Position.BUY -> (pick.distFromLow52w ?: return false) >= 15.0 && close
            // SELL 격상 (Stage 4): 고점 -15% 이하 + 종가 < MA20
            Position.SELL -> (pick.distFromHigh52w ?: return false) <= -15.0 && !close
        }
    }

    // 격하 시그널 없음 — 진입 시그널 전용 모듈.
    override fun shouldDemote(pick: StockPick, side: Position): Boolean = false
}
