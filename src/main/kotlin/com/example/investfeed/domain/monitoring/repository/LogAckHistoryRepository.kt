package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.AckSourceType
import com.example.investfeed.domain.monitoring.entity.LogAckHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface LogAckHistoryRepository : JpaRepository<LogAckHistory, Long> {

    fun findBySourceTypeAndSourceIdOrderByActedAtDesc(
        sourceType: AckSourceType,
        sourceId: Long,
    ): List<LogAckHistory>

    @Modifying
    @Query("DELETE FROM LogAckHistory h WHERE h.actedAt < :threshold")
    fun deleteByActedAtBefore(@Param("threshold") threshold: LocalDateTime): Int
}
