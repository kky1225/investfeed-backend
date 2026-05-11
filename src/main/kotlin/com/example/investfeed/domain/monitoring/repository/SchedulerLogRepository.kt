package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SchedulerLogRepository : JpaRepository<SchedulerLog, Long>, JpaSpecificationExecutor<SchedulerLog> {
    fun countByAcknowledgedFalseAndStatusNot(status: String): Long
    fun findByAcknowledgedFalseAndStatusNot(status: String): List<SchedulerLog>
    fun findByIdInAndAcknowledgedFalseAndStatusNot(ids: Collection<Long>, status: String): List<SchedulerLog>

    @Modifying
    @Query("DELETE FROM SchedulerLog s WHERE s.startedAt < :threshold")
    fun deleteByStartedAtBefore(@Param("threshold") threshold: LocalDateTime): Int

    fun existsBySchedulerNameAndStatusInAndStartedAtAfterAndAcknowledgedFalse(
        schedulerName: String,
        statuses: Collection<String>,
        startedAt: LocalDateTime,
    ): Boolean

    fun findBySchedulerNameAndStatusInAndStartedAtAfter(
        schedulerName: String,
        statuses: Collection<String>,
        startedAt: LocalDateTime,
    ): List<SchedulerLog>
}
