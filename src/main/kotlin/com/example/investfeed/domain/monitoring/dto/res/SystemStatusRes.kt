package com.example.investfeed.domain.monitoring.dto.res

data class SystemStatusRes(
    val dbStatus: String,   // UP / DOWN
    val redisStatus: String,
    val heapUsedMb: Long,
    val heapMaxMb: Long,
    val heapUsagePercent: Int,
    val uptimeSec: Long,
    val jvmThreads: Int,                // 현재 활성 스레드 수 (전체)
    val tomcatActive: Int,              // Tomcat 사용 중인 워커 수 (-1 = 조회 실패)
    val tomcatMax: Int,                 // Tomcat 최대 워커 수
    val fastSchedulerActive: Int,       // fastScheduler 실행 중 태스크 수
    val fastSchedulerMax: Int,          // fastScheduler 풀 사이즈
    val slowSchedulerActive: Int,       // slowScheduler 실행 중 태스크 수
    val slowSchedulerMax: Int,          // slowScheduler 풀 사이즈
)
