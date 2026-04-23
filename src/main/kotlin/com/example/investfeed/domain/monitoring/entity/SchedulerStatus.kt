package com.example.investfeed.domain.monitoring.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "scheduler_status")
class SchedulerStatus(
    @Id
    @Column(name = "scheduler_name", length = 100)
    val schedulerName: String,

    @Column(nullable = false, length = 10)
    var schedulerType: String, // FAST / SLOW

    @Column(nullable = false)
    var timeoutSec: Int = 180,

    @Column
    var lastStartedAt: LocalDateTime? = null,

    @Column
    var lastFinishedAt: LocalDateTime? = null,

    @Column
    var lastSuccessAt: LocalDateTime? = null,

    @Column
    var lastSuccessDurationMs: Long? = null,

    @Column
    var lastFailureAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var lastFailureMessage: String? = null,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
