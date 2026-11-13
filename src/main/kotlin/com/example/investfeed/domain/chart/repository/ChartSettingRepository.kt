package com.example.investfeed.domain.chart.repository

import com.example.investfeed.domain.chart.entity.ChartSetting
import org.springframework.data.jpa.repository.JpaRepository

interface ChartSettingRepository : JpaRepository<ChartSetting, Long> {

    fun findByMemberId(memberId: Long): ChartSetting?
}
