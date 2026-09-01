package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import com.example.investfeed.domain.monitoring.entity.SchedulerStatus
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.repository.SchedulerLogRepository
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SchedulerRecordWriter(
    private val schedulerLogRepository: SchedulerLogRepository,
    private val schedulerStatusRepository: SchedulerStatusRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateFiredAt(scheduler: SchedulerName, at: LocalDateTime) {
        val entity = loadOrCreate(scheduler)
        entity.lastFiredAt = at
        save(entity)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markStarted(scheduler: SchedulerName, at: LocalDateTime) {
        val entity = loadOrCreate(scheduler)
        entity.lastStartedAt = at
        save(entity)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertSuccess(scheduler: SchedulerName, finishedAt: LocalDateTime, durationMs: Long) {
        val entity = loadOrCreate(scheduler)
        entity.lastFinishedAt = finishedAt
        entity.lastSuccessAt = finishedAt
        entity.lastSuccessDurationMs = durationMs
        save(entity)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsertFailure(scheduler: SchedulerName, finishedAt: LocalDateTime, message: String?) {
        val entity = loadOrCreate(scheduler)
        entity.lastFinishedAt = finishedAt
        entity.lastFailureAt = finishedAt
        entity.lastFailureMessage = message
        save(entity)
    }

    /** 실행 이력 1건 기록. 업무 트랜잭션이 롤백돼도 남아야 하므로 상태 기록과 동일하게 독립 트랜잭션. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun appendLog(
        scheduler: SchedulerName,
        startedAt: LocalDateTime,
        finishedAt: LocalDateTime,
        durationMs: Long,
        status: String,
        errorMessage: String?,
    ) {
        schedulerLogRepository.save(
            SchedulerLog(
                schedulerName = scheduler.name,
                startedAt = startedAt,
                finishedAt = finishedAt,
                durationMs = durationMs,
                status = status,
                errorMessage = errorMessage,
            )
        )
    }

    private fun loadOrCreate(scheduler: SchedulerName): SchedulerStatus =
        schedulerStatusRepository.findById(scheduler.name).orElse(
            SchedulerStatus(schedulerName = scheduler.name, schedulerType = scheduler.type.name)
        ).also { it.schedulerType = scheduler.type.name }

    private fun save(entity: SchedulerStatus) {
        entity.updatedAt = LocalDateTime.now()
        schedulerStatusRepository.save(entity)
    }
}
