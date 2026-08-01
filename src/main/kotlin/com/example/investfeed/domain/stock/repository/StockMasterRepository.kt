package com.example.investfeed.domain.stock.repository

import com.example.investfeed.domain.stock.entity.StockMaster
import org.springframework.data.jpa.repository.JpaRepository

interface StockMasterRepository : JpaRepository<StockMaster, Long> {
    fun findTop20ByStkNmContainingIgnoreCase(keyword: String): List<StockMaster>
    fun findByStkCdIn(stkCds: Collection<String>): List<StockMaster>
}