package com.example.investfeed.domain.assistant.dto.req

data class AssistantSettingReq(
    val krEnabled: Boolean,
    val usEnabled: Boolean,
    val coinEnabled: Boolean,
    val sectionsOff: List<String> = emptyList(),
)
