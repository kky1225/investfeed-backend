package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.ApiCallDailyStat
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ApiCallDailyStatRepository : JpaRepository<ApiCallDailyStat, Long> {
    fun findAllByStatDateIn(dates: Collection<LocalDate>): List<ApiCallDailyStat>
}
