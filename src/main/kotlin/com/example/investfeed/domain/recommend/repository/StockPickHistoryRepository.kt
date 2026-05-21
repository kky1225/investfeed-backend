package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockPickHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.time.LocalDateTime

interface StockPickHistoryRepository : JpaRepository<StockPickHistory, Long> {
    fun findByStkCdInAndPickDateAfterOrderByStkCdAscPickDateDesc(
        stkCds: List<String>,
        pickDateAfter: LocalDateTime,
    ): List<StockPickHistory>

    @Query("SELECT DISTINCT CAST(h.pickDate AS LocalDate) FROM StockPickHistory h WHERE h.pickDate > :after")
    fun findDistinctPickDatesAfter(after: LocalDateTime): List<LocalDate>

    /** 특정 시각 범위 내 추천 이력 — BacktestBackfillService 가 영업일별 history 묶음 조회용. */
    fun findByPickDateBetween(start: LocalDateTime, end: LocalDateTime): List<StockPickHistory>

    /**
     * 해당 시각 범위(= 그날 00:00~23:59:59) 이력 일괄 삭제.
     * RecommendScheduler 가 같은 날 재실행될 때, 저장 직전 "오늘분"을 비우고 최신 실행분으로
     * 교체하기 위함(stk_cd·일자당 1건 보장 — stock_pick 전량교체와 동일 의미를 history 오늘분에 적용).
     */
    @Modifying
    @Query("DELETE FROM StockPickHistory h WHERE h.pickDate >= :start AND h.pickDate <= :end")
    fun deleteByPickDateBetween(start: LocalDateTime, end: LocalDateTime)

    /**
     * 일별 백필 진행도 — pick_date 별 총 개수 / priceClose 1d/5d/20d 채움 개수 집계.
     * 관리자 모니터링 페이지 ("백필 진행도" 탭) 가 사용.
     *
     * 반환 row 순서: [pickDateLocalDate(LocalDate), total(Long), filled1d(Long), filled5d(Long), filled20d(Long)]
     * — interface projection 없이 Object[] 로 받아서 service 에서 매핑.
     */
    @Query("""
        SELECT CAST(h.pickDate AS LocalDate) AS pickDay,
               COUNT(h) AS total,
               SUM(CASE WHEN h.priceClose1d IS NOT NULL THEN 1L ELSE 0L END) AS filled1d,
               SUM(CASE WHEN h.priceClose5d IS NOT NULL THEN 1L ELSE 0L END) AS filled5d,
               SUM(CASE WHEN h.priceClose20d IS NOT NULL THEN 1L ELSE 0L END) AS filled20d
        FROM StockPickHistory h
        WHERE h.pickDate >= :after
        GROUP BY CAST(h.pickDate AS LocalDate)
        ORDER BY CAST(h.pickDate AS LocalDate) DESC
    """)
    fun aggregateBackfillStatusAfter(after: LocalDateTime): List<Array<Any>>
}
