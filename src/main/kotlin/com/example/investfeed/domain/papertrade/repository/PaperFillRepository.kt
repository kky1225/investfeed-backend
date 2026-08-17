package com.example.investfeed.domain.papertrade.repository

import com.example.investfeed.domain.papertrade.entity.PaperFill
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PaperFillRepository : JpaRepository<PaperFill, Long> {
    fun findFirstByStkCdAndSideInOrderByFillDateDescIdDesc(
        stkCd: String,
        sides: Collection<String>,
    ): PaperFill?

    fun findByFillDateAndSide(fillDate: LocalDate, side: String): List<PaperFill>
    fun findByFillDate(fillDate: LocalDate): List<PaperFill>
}