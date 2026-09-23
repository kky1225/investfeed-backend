package com.example.investfeed.domain.assistant.dto.req

data class AssistantSettingReq(
    val krEnabled: Boolean,
    val usEnabled: Boolean,
    val coinEnabled: Boolean,
    val krWarnEnabled: Boolean = true,
    val usWarnEnabled: Boolean = true,
    val releaseAlertEnabled: Boolean = true,
    val sectionsOff: List<String> = emptyList(),
)
