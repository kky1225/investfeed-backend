package com.example.investfeed.domain.recommend.repository

import com.example.investfeed.domain.recommend.entity.MarketIndexSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MarketIndexSnapshotRepository : JpaRepository<MarketIndexSnapshot, Long> {
    fun findByCapturedDate(capturedDate: LocalDate): MarketIndexSnapshot?

    fun findByCapturedDateAfterOrderByCapturedDateDesc(after: LocalDate): List<MarketIndexSnapshot>
}
