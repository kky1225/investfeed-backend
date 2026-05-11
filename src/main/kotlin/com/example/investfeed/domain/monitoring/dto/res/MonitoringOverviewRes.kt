package com.example.investfeed.domain.monitoring.dto.res

import org.springframework.data.domain.Page

/**
 * 모니터링 페이지 탭별 통합 응답 DTO 모음.
 *
 * - 각 탭이 1번의 GET 호출로 그 탭 렌더링에 필요한 모든 데이터를 받도록 설계.
 * - `unackCount` 는 모든 탭에 공통 포함 — 헤더의 미확인 배지가 탭과 무관하게 항상 노출되므로,
 *   탭 fetch 한 번에 같이 갱신되도록 묶음.
 */
data class SchedulerOverviewRes(
    val catalog: List<SchedulerCatalogRes>,
    val statuses: List<SchedulerStatusRes>,
    val logs: Page<SchedulerLogRes>,
    val unackCount: UnacknowledgedCountRes,
    val isHoliday: Boolean,
)

data class ConfigLogsOverviewRes(
    val logs: Page<SchedulerConfigLogRes>,
    val unackCount: UnacknowledgedCountRes,
)

data class RedisOverviewRes(
    val redis: RedisCacheRes,
    val unackCount: UnacknowledgedCountRes,
)

data class ErrorLogsOverviewRes(
    val logs: Page<ErrorLogRes>,
    val unackCount: UnacknowledgedCountRes,
)

data class ApiCallsOverviewRes(
    val stats: ApiCallStatsRes,
    val unackCount: UnacknowledgedCountRes,
)

data class SystemOverviewRes(
    val system: SystemStatusRes,
    val unackCount: UnacknowledgedCountRes,
)
