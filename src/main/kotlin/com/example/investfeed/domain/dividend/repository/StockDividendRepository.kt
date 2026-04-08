package com.example.investfeed.domain.dividend.repository

import com.example.investfeed.domain.dividend.entity.StockDividend
import org.springframework.data.jpa.repository.JpaRepository

interface StockDividendRepository : JpaRepository<StockDividend, Long> {
    fun findByStkCdAndDvdnBasDtGreaterThanEqualAndStckDvdnRcdNmNotOrderByDvdnBasDtDesc(stkCd: String, dvdnBasDt: String, stckDvdnRcdNm: String): List<StockDividend>
    fun findFirstByStkCdAndTypeOrderByCreatedDtDesc(stkCd: String, type: String): StockDividend?
}
