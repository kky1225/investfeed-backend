package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.SchedulerStatus
import java.time.LocalDateTime

data class SchedulerStatusRes(
    val schedulerName: String,
    val schedulerType: String,
    val timeoutSec: Int,
    val state: String,              // SUCCESS / WARNING / FAILED / STUCK / PENDING
    val fireStatus: String,         // FIRED / MISSED / NONE — 이번 사이클(직전 22:00~) 발화 여부
    val lastFiredAt: LocalDateTime?,
    val lastStartedAt: LocalDateTime?,
    val lastFinishedAt: LocalDateTime?,
    val lastSuccessAt: LocalDateTime?,
    val lastSuccessDurationMs: Long?,
    val lastFailureAt: LocalDateTime?,
    val lastFailureMessage: String?,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(e: SchedulerStatus, state: String, fireStatus: String = "NONE") = SchedulerStatusRes(
            schedulerName = e.schedulerName,
            schedulerType = e.schedulerType,
            timeoutSec = e.timeoutSec,
            state = state,
            fireStatus = fireStatus,
            lastFiredAt = e.lastFiredAt,
            lastStartedAt = e.lastStartedAt,
            lastFinishedAt = e.lastFinishedAt,
            lastSuccessAt = e.lastSuccessAt,
            lastSuccessDurationMs = e.lastSuccessDurationMs,
            lastFailureAt = e.lastFailureAt,
            lastFailureMessage = e.lastFailureMessage,
            updatedAt = e.updatedAt,
        )
    }
}
