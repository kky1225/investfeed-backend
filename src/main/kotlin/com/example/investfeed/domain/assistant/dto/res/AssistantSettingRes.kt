package com.example.investfeed.domain.assistant.dto.res

data class AssistantSettingRes(
    val krEnabled: Boolean,
    val usEnabled: Boolean,
    val coinEnabled: Boolean,
    val krWarnEnabled: Boolean,
    val usWarnEnabled: Boolean,
    val releaseAlertEnabled: Boolean,
    val sectionsOff: List<String>,
)
