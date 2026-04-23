package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.SchedulerLog
import java.time.LocalDateTime

data class SchedulerLogRes(
    val id: Long,
    val schedulerName: String,
    val startedAt: LocalDateTime,
    val finishedAt: LocalDateTime?,
    val durationMs: Long?,
    val status: String,
    val errorMessage: String?,
    val acknowledged: Boolean,
    val acknowledgedBy: Long?,
    val acknowledgedByName: String?,
    val acknowledgedAt: LocalDateTime?,
    val acknowledgeNote: String?,
) {
    companion object {
        fun from(e: SchedulerLog, acknowledgedByName: String? = null) = SchedulerLogRes(
            id = e.id,
            schedulerName = e.schedulerName,
            startedAt = e.startedAt,
            finishedAt = e.finishedAt,
            durationMs = e.durationMs,
            status = e.status,
            errorMessage = e.errorMessage,
            acknowledged = e.acknowledged,
            acknowledgedBy = e.acknowledgedBy,
            acknowledgedByName = acknowledgedByName,
            acknowledgedAt = e.acknowledgedAt,
            acknowledgeNote = e.acknowledgeNote,
        )
    }
}
