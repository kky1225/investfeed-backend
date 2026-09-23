package com.example.investfeed.domain.calendar.scheduler

import com.example.investfeed.domain.assistant.service.ReleaseAlertService
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component

@Component
class CalendarSyncScheduler(
    private val economicCalendarService: EconomicCalendarService,
    private val releaseAlertService: ReleaseAlertService,
    private val schedulerLogService: SchedulerLogService,
    @Qualifier("slowScheduler") private val slowScheduler: ThreadPoolTaskScheduler,
) {
    private val log = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun initSync() {
        slowScheduler.execute {
            if (economicCalendarService.isCacheWarm()) {
                log.info { "CalendarSync 초기 warming skip — Redis 캐시 이미 존재" }
                detectReleases()   // 캐시가 있으면 동기화 없이 발표 감지만 (다운타임 중 발표 확인)
                return@execute
            }
            syncCalendarData()
        }
    }

    @Scheduled(cron = SchedulerCron.CALENDAR_SYNC, scheduler = "slowScheduler")
    fun syncCalendarData() {
        schedulerLogService.execute(SchedulerName.CalendarSyncScheduler) {
            val start = System.currentTimeMillis()
            try {
                economicCalendarService.syncCurrentData()
            } catch (e: Exception) {
                log.error { "CalendarSyncScheduler 실행 실패: ${e.message}" }
                throw e
            } finally {
                log.info { "CalendarSyncScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
            }
        }
        detectReleases()
    }

    private fun detectReleases() {
        runCatching { releaseAlertService.run() }
            .onSuccess { r -> log.info { "발표 알림 감지 완료: scanned=${r.scanned} saved=${r.saved} suppressed=${r.suppressed} posted=${r.posted}${if (r.seed) " (seed)" else ""}" } }
            .onFailure { log.error(it) { "발표 알림 감지 실패" } }
    }
}
