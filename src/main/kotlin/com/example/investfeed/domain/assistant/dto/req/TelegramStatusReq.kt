package com.example.investfeed.domain.assistant.dto.req

import com.example.investfeed.domain.assistant.entity.TelegramStatus

/** 웹 스위치. ACTIVE(켬) / PAUSED(끔) 만 허용 — 연결·해제·차단은 별도 경로 */
data class TelegramStatusReq(
    val status: TelegramStatus,
)
