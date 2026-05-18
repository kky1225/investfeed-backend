package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick
import org.springframework.stereotype.Component

/**
 * 거래량-가격 보정 모듈 (Volume-Price Analysis, VPA).
 *
 * 종목 당일 등락률 + 거래량(RVOL) 4분면 매트릭스 — 사용자 인용 패턴
 * (거래량 강한 양봉=Healthy uptrend, 거래량 강한 음봉=Distribution) 그대로 시스템화.
 *
 * **거래량 강할 때만 보정 발동** (RVOL > 2.0). 약한 거래량은 무보정 — 다른 모듈에 영향력 양보.
 *
 * | 종목 당일 등락 | 종목 RVOL | BUY 진영 | SELL 진영 |
 * |---|---|---|---|
 * | 상승 | 강함 (>2.0) | 격상 | 격하 |
 * | 상승 | 약함 | 무보정 | 무보정 |
 * | 하락 | 강함 (>2.0) | 격하 | 격상 |
 * | 하락 | 약함 | 무보정 | 무보정 |
 */
@Component
class VolumePriceModule : AdjustmentModule {
    override val name = "VolumePrice"

    override fun isEnabled(setting: RecommendSetting): Boolean = setting.volumePriceEnabled

    override fun shouldPromote(pick: StockPick, side: Position): Boolean {
        if (!isStrongVolume(pick)) return false
        val rate = pick.todayChangeRate ?: return false

        return when (side) {
            // BUY 진영: 상승 + 거래량 강함 → 격상 (Healthy uptrend)
            Position.BUY -> rate > 0
            // SELL 진영: 하락 + 거래량 강함 → 격상 (강한 매도 추세 확정)
            Position.SELL -> rate < 0
        }
    }

    override fun shouldDemote(pick: StockPick, side: Position): Boolean {
        if (!isStrongVolume(pick)) return false
        val rate = pick.todayChangeRate ?: return false

        return when (side) {
            // BUY 진영: 하락 + 거래량 강함 → 격하 (Distribution)
            Position.BUY -> rate < 0
            // SELL 진영: 상승 + 거래량 강함 → 격하 (강한 매수 반전 신호)
            Position.SELL -> rate > 0
        }
    }

    /**
     * 종목 당일 거래량이 20일 평균 거래량의 [RVOL_THRESHOLD] 배 이상인지.
     * 데이터 부족 시 false (거래량 약함으로 간주 → 무보정).
     */
    private fun isStrongVolume(pick: StockPick): Boolean {
        val today = pick.todayVolume ?: return false
        val avg = pick.avg20dVolume ?: return false
        if (avg <= 0L) return false
        return today.toDouble() / avg >= RvolThreshold.VALUE
    }
}
