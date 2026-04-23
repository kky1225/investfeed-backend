package com.example.investfeed.domain.monitoring.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "scheduler_config_log")
class SchedulerConfigLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    val schedulerName: String,

    @Column(nullable = false, length = 50)
    val fieldName: String,   // 'timeout_sec' 등

    @Column(length = 200)
    val oldValue: String? = null,

    @Column(nullable = false, length = 200)
    val newValue: String,

    @Column(nullable = false)
    val changedBy: Long,     // members.id

    @Column(nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    @Column(length = 500)
    val reason: String? = null,
)
