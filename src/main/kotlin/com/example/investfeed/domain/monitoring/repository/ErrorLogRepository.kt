package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.ErrorLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ErrorLogRepository : JpaRepository<ErrorLog, Long>, JpaSpecificationExecutor<ErrorLog> {
    fun countByAcknowledgedFalse(): Long
    fun findByAcknowledgedFalse(): List<ErrorLog>
    fun findByIdInAndAcknowledgedFalse(ids: Collection<Long>): List<ErrorLog>
    fun findTopBySchedulerNameAndOccurredAtBetweenOrderByOccurredAtDesc(
        schedulerName: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ): ErrorLog?

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.occurredAt < :threshold")
    fun deleteByOccurredAtBefore(@Param("threshold") threshold: LocalDateTime): Int
}
