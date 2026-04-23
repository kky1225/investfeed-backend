package com.example.investfeed.domain.monitoring.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "scheduler_log")
class SchedulerLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val schedulerName: String,

    @Column(nullable = false)
    val startedAt: LocalDateTime,

    @Column
    var finishedAt: LocalDateTime? = null,

    @Column
    var durationMs: Long? = null,

    @Column(nullable = false, length = 20)
    var status: String, // SUCCESS / FAILED / INTERRUPTED

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    /**
     * 관리자가 "확인함" 처리한 이벤트는 true.
     * computeState 에서 WARNING 판정 대상에서 제외된다.
     */
    @Column(nullable = false)
    var acknowledged: Boolean = false,

    @Column
    var acknowledgedBy: Long? = null,   // members.id

    @Column
    var acknowledgedAt: LocalDateTime? = null,

    @Column(length = 500)
    var acknowledgeNote: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
