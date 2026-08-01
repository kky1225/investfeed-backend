package com.example.investfeed.domain.us.stock.repository

import com.example.investfeed.domain.us.stock.entity.UsStockMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UsStockMasterRepository : JpaRepository<UsStockMaster, Long> {

    @Query(
        """
        SELECT u FROM UsStockMaster u
        WHERE LOWER(u.stkNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.stkEnm) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.stkCd) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """
    )
    fun search(@Param("keyword") keyword: String, pageable: Pageable): List<UsStockMaster>

    fun findByStexTpAndStkCd(stexTp: String, stkCd: String): UsStockMaster?
}
