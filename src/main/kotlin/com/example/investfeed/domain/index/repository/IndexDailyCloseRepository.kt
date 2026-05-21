package com.example.investfeed.domain.index.repository

import com.example.investfeed.domain.index.entity.IndexDailyClose
import org.springframework.data.jpa.repository.JpaRepository

interface IndexDailyCloseRepository : JpaRepository<IndexDailyClose, Long> {
    fun existsByIndsCdAndDt(indsCd: String, dt: String): Boolean
    fun findByIndsCdAndDt(indsCd: String, dt: String): IndexDailyClose?
    fun findFirstByIndsCdOrderByDtDesc(indsCd: String): IndexDailyClose?

    /** 운용 시작일(YYYYMMDD) 이상 첫 영업일 종가 — 벤치마크 시작점. */
    fun findFirstByIndsCdAndDtGreaterThanEqualOrderByDtAsc(indsCd: String, dt: String): IndexDailyClose?
}
