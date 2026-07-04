package com.example.investfeed.domain.index.repository

import com.example.investfeed.domain.index.entity.IndexInvestorDaily
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface IndexInvestorDailyRepository : JpaRepository<IndexInvestorDaily, Long> {
    fun findByIndsCdOrderByDtDesc(indsCd: String, pageable: Pageable): List<IndexInvestorDaily>
    fun existsByIndsCdAndDt(indsCd: String, dt: String): Boolean
    fun findFirstByIndsCdOrderByDtDesc(indsCd: String): IndexInvestorDaily?
    fun findByIndsCdAndDtGreaterThanEqualOrderByDtAsc(indsCd: String, dt: String): List<IndexInvestorDaily>
}
