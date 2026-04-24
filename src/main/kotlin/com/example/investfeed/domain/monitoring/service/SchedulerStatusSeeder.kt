package com.example.investfeed.domain.monitoring.service

import com.example.investfeed.domain.monitoring.entity.SchedulerStatus
import com.example.investfeed.domain.monitoring.repository.SchedulerStatusRepository
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 앱 기동 시 scheduler_status 테이블에 누락된 스케줄러 row 를 자동 등록한다.
 *
 * - SQL seed(investfeed.sql 의 `INSERT ... ON CONFLICT DO NOTHING`)는 신규 DB 설치 시점에만 동작.
 *   이미 초기화된 운영/개발 DB 에 새 스케줄러 row 를 자동 반영하기 위한 보완 장치.
 * - 실행 로직과 무관: 기존 @Scheduled cron 은 그대로 동작, 이 Seeder 는 테이블 row 존재만 보장.
 * - 기존 row 는 건드리지 않음 (관리자가 조정한 timeout 보존).
 * - 기동 실패 격리: 내부 try-catch 로 Seeder 실패가 앱 기동을 막지 않음.
 *
 * 새 스케줄러 추가 시:
 * 1. CATALOG 에 한 줄 추가
 * 2. investfeed.sql seed 에도 한 줄 추가 (신규 DB 설치 대비)
 * 3. 앱 재시작 → 모든 환경 자동 동기화
 */
@Component
class SchedulerStatusSeeder(
    private val schedulerStatusRepository: SchedulerStatusRepository,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    data class CatalogEntry(val name: String, val type: String, val defaultTimeoutSec: Int)

    companion object {
        val CATALOG: List<CatalogEntry> = listOf(
            CatalogEntry("PriceAlertScheduler",          "FAST", 60),
            CatalogEntry("MarketIndexScheduler",         "FAST", 60),
            CatalogEntry("InvestorCloseMarketScheduler", "FAST", 60),
            CatalogEntry("GoalAlertScheduler",           "SLOW", 60),
            CatalogEntry("RebalancingAlertScheduler",    "SLOW", 60),
            CatalogEntry("StockDividendScheduler",       "SLOW", 60),
            CatalogEntry("ApiKeyExpiryScheduler",        "SLOW", 60),
            CatalogEntry("SchedulerLogCleanupScheduler", "SLOW", 60),
            CatalogEntry("RecommendScheduler",           "SLOW", 120),
            CatalogEntry("IndexInvestorDailyScheduler",  "SLOW", 120),
            CatalogEntry("HolidayRefreshScheduler",      "SLOW", 120),
            CatalogEntry("CalendarSyncScheduler",        "SLOW", 300),
            CatalogEntry("InterestSyncScheduler",        "SLOW", 600),
            CatalogEntry("HoldingSyncScheduler",         "SLOW", 600),
        )
    }

    override fun run(args: ApplicationArguments) {
        try {
            seed()
        } catch (e: Exception) {
            // 기동 실패 격리: Seeder 실패해도 앱은 정상 기동
            log.error(e) { "[startup] SchedulerStatusSeeder 실패: ${e.message}" }
        }
    }

    @Transactional
    internal fun seed() {
        var inserted = 0
        CATALOG.forEach { entry ->
            if (!schedulerStatusRepository.existsById(entry.name)) {
                schedulerStatusRepository.save(
                    SchedulerStatus(
                        schedulerName = entry.name,
                        schedulerType = entry.type,
                        timeoutSec = entry.defaultTimeoutSec,
                    )
                )
                inserted++
                log.info { "[startup] scheduler_status 신규 등록: ${entry.name} (${entry.type}, timeout=${entry.defaultTimeoutSec}s)" }
            }
        }
        if (inserted > 0) {
            log.info { "[startup] SchedulerStatusSeeder 완료: ${inserted}건 신규 등록" }
        }
    }
}
