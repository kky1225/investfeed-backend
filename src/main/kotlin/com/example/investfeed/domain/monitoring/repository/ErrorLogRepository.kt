package com.example.investfeed.domain.monitoring.repository

import com.example.investfeed.domain.monitoring.entity.ErrorLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ErrorLogRepository : JpaRepository<ErrorLog, Long> {
    fun findAllByOrderByOccurredAtDesc(pageable: Pageable): Page<ErrorLog>

    /**
     * Shutdown recovery 에서 특정 스케줄러가 낸 에러 중 시간 범위 내 가장 최근 것 1건 조회.
     * MDC 로 주입된 scheduler_name 이 정확히 일치하는 것만 매칭.
     */
    fun findTopBySchedulerNameAndOccurredAtBetweenOrderByOccurredAtDesc(
        schedulerName: String,
        start: LocalDateTime,
        end: LocalDateTime,
    ): ErrorLog?

    @Modifying
    @Query("DELETE FROM ErrorLog e WHERE e.occurredAt < :threshold")
    fun deleteByOccurredAtBefore(@Param("threshold") threshold: LocalDateTime): Int
}
