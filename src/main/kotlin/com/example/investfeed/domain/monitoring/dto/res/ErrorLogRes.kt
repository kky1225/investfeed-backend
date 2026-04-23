package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.ErrorLog
import java.time.LocalDateTime

data class ErrorLogRes(
    val id: Long,
    val occurredAt: LocalDateTime,
    val loggerName: String,
    val threadName: String?,
    val message: String?,
    val stackTrace: String?,
    val acknowledged: Boolean,
    val acknowledgedBy: Long?,
    val acknowledgedByName: String?,
    val acknowledgedAt: LocalDateTime?,
    val acknowledgeNote: String?,
) {
    companion object {
        fun from(e: ErrorLog, acknowledgedByName: String? = null) = ErrorLogRes(
            id = e.id,
            occurredAt = e.occurredAt,
            loggerName = e.loggerName,
            threadName = e.threadName,
            message = e.message,
            stackTrace = e.stackTrace,
            acknowledged = e.acknowledged,
            acknowledgedBy = e.acknowledgedBy,
            acknowledgedByName = acknowledgedByName,
            acknowledgedAt = e.acknowledgedAt,
            acknowledgeNote = e.acknowledgeNote,
        )
    }
}
