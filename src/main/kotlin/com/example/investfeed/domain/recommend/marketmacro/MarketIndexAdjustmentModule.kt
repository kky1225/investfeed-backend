package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.domain.recommend.entity.StockPick
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 코스피/코스닥 매크로 보정 모듈 — **3 시그널 만장일치 룰**.
 *
 * 사용자 추천 조회 시점에 [adjust] 호출 → Redis 캐시에서 매크로 스냅샷 조회 →
 * 지수 등락 + 기관 매매 + 외국인 매매 **3 시그널이 모두 같은 방향일 때만** 보정.
 *
 * **만장일치 룰**:
 *
 * | 시장 상태 | 지수 | 기관 | 외국인 | BUY 진영 | SELL 진영 |
 * |---|---|---|---|---|---|
 * | **강세 만장일치** | 상승 | 매수 | 매수 | **격상** (BUY → STRONG_BUY) | **격하** (SELL → HOLD) |
 * | **약세 만장일치** | 하락 | 매도 | 매도 | **격하** (BUY → HOLD) | **격상** (SELL → STRONG_SELL) |
 * | 그 외 (다이버전스/혼합) | — | — | — | 유지 | 유지 |
 *
 * **다이버전스 = 시장 신호 명확하지 않음 = 보정 X** (보수적 접근, 단일일 노이즈 회피).
 *
 * **동행지표 적용 시간 가드 — 같은 거래일 && 22:00 이전**:
 *  - 매크로는 동행지표지만, NXT 종목은 20:00까지 매매 가능 → 정규장 마감(15:30) 후에도
 *    "그날 시장 환경" 으로 참고 가치. 22:00 = 새 리포트 생성 시각이라 그 전까지 일관 유지.
 *  - 20:00 컷 시 같은 리포트 내 등급이 HOLD→BUY 로 점프 → 사용자가 "격상" 으로 오해.
 *    22:00 컷이면 점프가 리포트 교체와 일치 → 자연스럽게 인지.
 *  - 거래일 경과 (다음날) → 매크로 캐시 updatedAt 이 전일 → 자동 무보정 (17h lag 재발 차단).
 *  - 22:00 이후 → 새 리포트 생성 시점 → 무보정 (다음 거래일 폴링까지).
 *  - 사용자는 옵션 한 번 켜두면 됨 — 같은 거래일 22:00 까지 자동 반영, 그 외 자동 미반영.
 *
 * - HOLD 종목은 방향 정보 없으므로 보정 대상 아님
 * - 캐시 미스/marketType 없음 → 무보정 fallback
 * - 분류 방향 절대 안 바뀜 (BUY ↔ SELL 불가)
 */
@Component
class MarketIndexAdjustmentModule(
    private val marketMacroCacheService: MarketMacroCacheService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        /** 새 리포트 생성 시각 (RecommendScheduler cron 22:00) — 보정 적용 마지노선. */
        private val REPORT_REFRESH_TIME: LocalTime = LocalTime.of(22, 0)
    }

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

        // 적용 시간 가드 — 같은 거래일 && 22:00 이전만 보정.
        //  - updatedAt 날짜 ≠ 오늘 → 거래일 경과 (전일 매크로 stale) → 무보정 (lag 재발 차단)
        //  - 현재 ≥ 22:00 → 새 리포트 생성 시점 → 무보정
        val now = LocalDateTime.now()
        if (snapshot.updatedAt.toLocalDate() != now.toLocalDate()
            || !now.toLocalTime().isBefore(REPORT_REFRESH_TIME)
        ) {
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

        // 강세 만장일치 (지수 상승 + 기관 매수 + 외국인 매수)
        val bullishUnanimous = isUp && instBuy && foreignBuy
        // 약세 만장일치 (지수 하락 + 기관 매도 + 외국인 매도)
        val bearishUnanimous = isDown && instSell && foreignSell

        return when {
            // 강세 만장일치 → BUY 격상 / SELL 격하
            bullishUnanimous && isBuySide -> promoteOnce(currentType)
            bullishUnanimous && isSellSide -> demoteOnce(currentType)
            // 약세 만장일치 → SELL 격상 / BUY 격하
            bearishUnanimous && isSellSide -> promoteOnce(currentType)
            bearishUnanimous && isBuySide -> demoteOnce(currentType)
            // 다이버전스 (혼합) / 등락 미미 → 유지
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
