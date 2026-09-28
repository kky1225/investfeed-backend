package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "assistant_setting")
class AssistantSetting(
    @Id
    @Column(name = "member_id")
    val memberId: Long,

    @Column(name = "kr_enabled", nullable = false)
    var krEnabled: Boolean = true,

    @Column(name = "us_enabled", nullable = false)
    var usEnabled: Boolean = true,

    @Column(name = "coin_enabled", nullable = false)
    var coinEnabled: Boolean = true,

    @Column(name = "kr_warn_enabled", nullable = false)
    var krWarnEnabled: Boolean = true,      // 국내 지수 ±3%·±5% 장중 급변 알림

    @Column(name = "us_warn_enabled", nullable = false)
    var usWarnEnabled: Boolean = true,      // 미국 지수 ±3%·±5% 장중 급변 알림

    @Column(name = "release_alert_enabled", nullable = false)
    var releaseAlertEnabled: Boolean = true, // 지표 발표 알림. 서킷브레이커는 설정 없이 항상 발송

    @Column(name = "telegram_chat_id")
    var telegramChatId: Long? = null,        // 3단계. 미연결 NULL. 채팅 하나 = 회원 하나 (UNIQUE)

    @Enumerated(EnumType.STRING)
    @Column(name = "telegram_status", nullable = false, length = 10)
    var telegramStatus: TelegramStatus = TelegramStatus.NONE,   // 알림 종류별 텔레그램 설정은 없고 위 스위치를 그대로 따른다

    @Column(name = "telegram_linked_at")
    var telegramLinkedAt: LocalDateTime? = null,

    @Column(name = "last_seen_message_id", nullable = false)
    var lastSeenMessageId: Long = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

enum class TelegramStatus { NONE, ACTIVE, PAUSED, BLOCKED }