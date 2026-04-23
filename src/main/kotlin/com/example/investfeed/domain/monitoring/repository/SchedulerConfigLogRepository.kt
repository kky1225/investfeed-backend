package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.SchedulerConfigLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SchedulerConfigLogRepository : JpaRepository<SchedulerConfigLog, Long> {
    fun findAllByOrderByChangedAtDesc(pageable: Pageable): Page<SchedulerConfigLog>
    fun findBySchedulerNameOrderByChangedAtDesc(schedulerName: String, pageable: Pageable): Page<SchedulerConfigLog>
}
