package com.example.investfeed.domain.dividend.repository

import com.example.investfeed.domain.dividend.entity.StockDividend
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StockDividendRepository : JpaRepository<StockDividend, Long> {
    fun findByStkCdAndDvdnBasDtGreaterThanEqualAndStckDvdnRcdNmNotOrderByDvdnBasDtDesc(stkCd: String, dvdnBasDt: String, stckDvdnRcdNm: String): List<StockDividend>
    fun findFirstByStkCdAndTypeOrderByCreatedDtDesc(stkCd: String, type: String): StockDividend?

    /**
     * 중복 저장 방지용 기존 키 조회.
     * uk_dividend (stk_cd, dvdn_bas_dt, scrs_itms_kcd) 조합을 "stkCd|dvdnBasDt|scrsItmsKcd" 문자열로 반환한다.
     */
    @Query(
        """
        SELECT CONCAT(d.stkCd, '|', COALESCE(d.dvdnBasDt, ''), '|', COALESCE(d.scrsItmsKcd, ''))
        FROM StockDividend d
        WHERE d.type = :type AND d.dvdnBasDt >= :minDvdnBasDt
        """
    )
    fun findExistingKeysByTypeAndMinDvdnBasDt(
        @Param("type") type: String,
        @Param("minDvdnBasDt") minDvdnBasDt: String,
    ): List<String>

    /**
     * ETF 분배금은 scrsItmsKcd 없이 (stkCd, dvdnBasDt) 로 유일하므로 stkCd 단위로 일괄 조회.
     */
    @Query(
        """
        SELECT d.dvdnBasDt FROM StockDividend d
        WHERE d.type = :type AND d.stkCd = :stkCd AND d.dvdnBasDt IS NOT NULL
        """
    )
    fun findDvdnBasDtsByStkCdAndType(
        @Param("stkCd") stkCd: String,
        @Param("type") type: String,
    ): List<String>
}
