package com.example.investfeed.domain.recommend.admin.dto.res

import java.time.LocalDate

/**
 * 추천 시스템 모니터링 — 일별 백필 진행도.
 *
 * BacktestBackfillService 가 매일 22:30 채우는 가격 컬럼들의 NOT NULL 카운트.
 * 백필이 정상 작동하는지 매일 확인 가능.
 *
 * 가정:
 * - pickDate=T 인 history → T+1영업일에 price_open_1d/close_1d 채움
 * - T+5영업일에 price_close_5d
 * - T+20영업일에 price_close_20d
 */
data class AdminBackfillStatusRes(
    val pickDate: LocalDate,
    val totalCount: Long,        // 해당일 추천 종목 수
    val filled1d: Long,          // priceClose1d NOT NULL 개수
    val filled5d: Long,          // priceClose5d NOT NULL 개수
    val filled20d: Long,         // priceClose20d NOT NULL 개수
)
