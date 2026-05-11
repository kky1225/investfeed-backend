package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.StockPickHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface StockPickHistoryRepository : JpaRepository<StockPickHistory, Long> {
    fun findByStkCdInAndPickDateAfterOrderByStkCdAscPickDateDesc(
        stkCds: List<String>,
        pickDateAfter: LocalDateTime,
    ): List<StockPickHistory>
}
