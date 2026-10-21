package com.example.investfeed.internal.assistant.dto.req

data class RecommendListReq(
    val grade: String? = null,
    val date: String? = null,   // 미지정 = 현재 추천. 지정하면 그날 산출 기록
)
