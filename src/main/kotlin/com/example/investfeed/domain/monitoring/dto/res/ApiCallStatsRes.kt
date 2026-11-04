package com.example.investfeed.domain.monitoring.dto.res

import com.example.investfeed.domain.monitoring.entity.ApiCallDailyStat
import java.time.LocalDate

data class ApiCallStatsRes(
    val items: List<ApiCallStatsItemRes>,
)

data class ApiCallStatsItemRes(
    val provider: String,
    val label: String,
    val todayCount: Long,
    val dailyLimit: Long?,
    val usageRatio: Double?,
    val todayFailed: Long,
    val todayRetried: Long,
    val todayAvgLatencyMs: Long?,
    val todayMaxLatencyMs: Long?,
    val recent7Days: List<DailyCallCount>,
)

data class DailyCallCount(
    val date: String,
    val count: Long,
    val failed: Long,
) {
    companion object {
        /** 지난 날짜는 DB 값을 쓰고, 아직 적재되지 않은 날짜와 오늘은 Redis 값을 쓴다 */
        fun of(date: LocalDate, redisCount: Long, redisFailed: Long, stored: ApiCallDailyStat?) = DailyCallCount(
            date = date.toString(),
            count = stored?.callCount ?: redisCount,
            failed = stored?.failedCount ?: redisFailed,
        )
    }
}
