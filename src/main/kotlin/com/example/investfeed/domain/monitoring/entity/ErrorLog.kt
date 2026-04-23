package com.example.investfeed.domain.monitoring.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "error_log")
class ErrorLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val occurredAt: LocalDateTime,

    @Column(nullable = false, length = 255)
    val loggerName: String,

    @Column(length = 100)
    val threadName: String? = null,

    /** 스케줄러 실행 중 발생한 에러의 경우 해당 스케줄러명 (MDC 로 자동 주입). */
    @Column(length = 100)
    val schedulerName: String? = null,

    @Column(columnDefinition = "TEXT")
    val message: String? = null,

    @Column(columnDefinition = "TEXT")
    val stackTrace: String? = null,

    @Column(nullable = false)
    var acknowledged: Boolean = false,

    @Column
    var acknowledgedBy: Long? = null,

    @Column
    var acknowledgedAt: LocalDateTime? = null,

    @Column(length = 500)
    var acknowledgeNote: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
