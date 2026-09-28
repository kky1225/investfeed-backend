package com.example.investfeed.domain.assistant.dto.res

import com.example.investfeed.domain.assistant.entity.TelegramStatus
import java.time.LocalDateTime

data class TelegramStatusRes(
    val configured: Boolean,          // 서버에 봇 토큰이 있는지. false 면 연결 버튼 대신 안내
    val status: TelegramStatus,
    val linkedAt: LocalDateTime?,
    val botUsername: String,
    val sendBlocked: Boolean,         // 관리자 전체 발송 차단 중
)

data class TelegramLinkCodeRes(
    val code: String,
    val expiresAt: LocalDateTime,
    val deepLink: String,             // https://t.me/{bot}?start={code} — 누르면 /start {code} 자동 입력
)
