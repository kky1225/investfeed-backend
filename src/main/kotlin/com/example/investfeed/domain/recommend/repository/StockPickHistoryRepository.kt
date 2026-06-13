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

    fun findByPickDateBetween(start: LocalDateTime, end: LocalDateTime): List<StockPickHistory>

    @Query("SELECT MAX(h.pickDate) FROM StockPickHistory h")
    fun findMaxPickDate(): LocalDateTime?

    @Modifying
    @Query("DELETE FROM StockPickHistory h WHERE h.pickDate >= :start AND h.pickDate <= :end")
    fun deleteByPickDateBetween(start: LocalDateTime, end: LocalDateTime)

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
