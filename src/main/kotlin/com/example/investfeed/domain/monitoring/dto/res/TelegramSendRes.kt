package com.example.investfeed.domain.monitoring.dto.res

data class TelegramSendRes(
    val configured: Boolean,   // 서버에 봇 토큰이 있는지
    val blocked: Boolean,      // 관리자 전체 발송 차단 중
)
