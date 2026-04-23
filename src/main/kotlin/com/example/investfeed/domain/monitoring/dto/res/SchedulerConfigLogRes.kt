package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.SchedulerConfigLog
import java.time.LocalDateTime

data class SchedulerConfigLogRes(
    val id: Long,
    val schedulerName: String,
    val fieldName: String,
    val oldValue: String?,
    val newValue: String,
    val changedBy: Long,
    val changedByName: String?,
    val changedAt: LocalDateTime,
    val reason: String?,
) {
    companion object {
        fun from(e: SchedulerConfigLog, changedByName: String?) = SchedulerConfigLogRes(
            id = e.id,
            schedulerName = e.schedulerName,
            fieldName = e.fieldName,
            oldValue = e.oldValue,
            newValue = e.newValue,
            changedBy = e.changedBy,
            changedByName = changedByName,
            changedAt = e.changedAt,
            reason = e.reason,
        )
    }
}
