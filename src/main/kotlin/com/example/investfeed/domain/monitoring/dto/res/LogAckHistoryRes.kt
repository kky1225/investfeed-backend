package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.LogAckHistory
import java.time.LocalDateTime

data class LogAckHistoryRes(
    val id: Long,
    val action: String,           // ACKNOWLEDGE / EDIT_NOTE / CANCEL
    val oldNote: String?,
    val newNote: String?,
    val actedBy: Long,
    val actedByName: String?,
    val actedAt: LocalDateTime,
) {
    companion object {
        fun from(e: LogAckHistory, actedByName: String?) = LogAckHistoryRes(
            id = e.id,
            action = e.action.name,
            oldNote = e.oldNote,
            newNote = e.newNote,
            actedBy = e.actedBy,
            actedByName = actedByName,
            actedAt = e.actedAt,
        )
    }
}
