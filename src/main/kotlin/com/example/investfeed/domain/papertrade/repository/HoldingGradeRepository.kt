package com.example.investfeed.domain.papertrade.repository

import com.example.investfeed.domain.papertrade.entity.HoldingGrade
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface HoldingGradeRepository : JpaRepository<HoldingGrade, Long> {
    fun findByStkCdAndEvalDate(stkCd: String, evalDate: LocalDate): HoldingGrade?
    fun findByEvalDate(evalDate: LocalDate): List<HoldingGrade>

    /** 가장 최근 평가일자 1건. 어드민 "보유 평가" 탭 기본값(evalDate 미지정 시) 용. */
    fun findFirstByOrderByEvalDateDesc(): HoldingGrade?
}
