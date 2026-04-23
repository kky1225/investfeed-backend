package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.SchedulerStatus
import org.springframework.data.jpa.repository.JpaRepository

interface SchedulerStatusRepository : JpaRepository<SchedulerStatus, String> {
    fun findAllByOrderBySchedulerNameAsc(): List<SchedulerStatus>
}
