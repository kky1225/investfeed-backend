package com.example.investfeed.domain.monitoring.dto.res

data class BulkAcknowledgeRes(
    val processedCount: Int,
    val appliedNote: String,
)
