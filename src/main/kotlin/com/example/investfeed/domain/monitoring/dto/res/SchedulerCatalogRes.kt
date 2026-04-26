package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.enum.SchedulerName

data class SchedulerCatalogRes(
    val schedulerName: String,
    val schedulerType: String,
    val defaultTimeoutSec: Int,
    val label: String,
) {
    companion object {
        fun from(e: SchedulerName) = SchedulerCatalogRes(
            schedulerName = e.name,
            schedulerType = e.type.name,
            defaultTimeoutSec = e.defaultTimeoutSec,
            label = e.label,
        )
    }
}
