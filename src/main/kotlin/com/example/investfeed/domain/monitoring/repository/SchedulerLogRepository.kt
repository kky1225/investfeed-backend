package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SchedulerLogRepository : JpaRepository<SchedulerLog, Long> {
    fun findAllByOrderByStartedAtDesc(pageable: Pageable): Page<SchedulerLog>
    fun findBySchedulerNameOrderByStartedAtDesc(schedulerName: String, pageable: Pageable): Page<SchedulerLog>
    fun findByStatusOrderByStartedAtDesc(status: String, pageable: Pageable): Page<SchedulerLog>
    fun findBySchedulerNameAndStatusOrderByStartedAtDesc(schedulerName: String, status: String, pageable: Pageable): Page<SchedulerLog>

    @Modifying
    @Query("DELETE FROM SchedulerLog s WHERE s.startedAt < :threshold")
    fun deleteByStartedAtBefore(@Param("threshold") threshold: LocalDateTime): Int

    fun existsBySchedulerNameAndStatusInAndStartedAtAfterAndAcknowledgedFalse(
        schedulerName: String,
        statuses: Collection<String>,
        startedAt: LocalDateTime,
    ): Boolean
}
