package com.example.investfeed.domain.recommend.adjustment

/**
 * 거래량 강함(RVOL = 당일 거래량 / 20일 평균 거래량) 판정 임계값.
 *
 * VolumePriceModule 과 BreakoutModule 이 공유 — "거래량 동반" 조건 단일 소스.
 * 향후 백테스트 데이터로 튜닝 시 이 한 곳만 변경.
 */
object RvolThreshold {
    const val VALUE = 2.0
}
