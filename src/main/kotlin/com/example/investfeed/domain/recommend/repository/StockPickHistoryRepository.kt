package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockPickHistory
import org.springframework.data.jpa.repository.JpaRepository
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
}
