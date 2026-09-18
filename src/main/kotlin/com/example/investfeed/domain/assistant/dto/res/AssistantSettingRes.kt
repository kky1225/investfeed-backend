package com.example.investfeed.domain.assistant.dto.res

data class AssistantSettingRes(
    val krEnabled: Boolean,
    val usEnabled: Boolean,
    val coinEnabled: Boolean,
    val sectionsOff: List<String>,
)
