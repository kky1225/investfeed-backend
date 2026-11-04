package com.example.investfeed.domain.monitoring.entity

import com.example.investfeed.domain.monitoring.service.ApiCallDailySnapshot
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "api_call_daily_stat",
    uniqueConstraints = [UniqueConstraint(columnNames = ["stat_date", "provider"])]
)
class ApiCallDailyStat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "stat_date", nullable = false)
    val statDate: LocalDate,

    @Column(nullable = false, length = 30)
    val provider: String, // ApiProvider.name

    @Column(nullable = false)
    var callCount: Long,

    @Column(nullable = false)
    var failedCount: Long,

    @Column(nullable = false)
    var retriedCount: Long,

    @Column(nullable = false)
    var latencyCount: Long, // 0 이면 응답 시간·실패 집계 도입 전의 날짜

    @Column(nullable = false)
    var latencyTotalMs: Long,

    @Column(nullable = false)
    var latencyMaxMs: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun overwrite(snapshot: ApiCallDailySnapshot) {
        callCount = snapshot.callCount
        failedCount = snapshot.failed
        retriedCount = snapshot.retried
        latencyCount = snapshot.latencyCount
        latencyTotalMs = snapshot.latencyTotalMs
        latencyMaxMs = snapshot.latencyMaxMs
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun from(snapshot: ApiCallDailySnapshot) = ApiCallDailyStat(
            statDate = snapshot.date,
            provider = snapshot.provider.name,
            callCount = snapshot.callCount,
            failedCount = snapshot.failed,
            retriedCount = snapshot.retried,
            latencyCount = snapshot.latencyCount,
            latencyTotalMs = snapshot.latencyTotalMs,
            latencyMaxMs = snapshot.latencyMaxMs,
        )
    }
}
