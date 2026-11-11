package com.example.investfeed.domain.calendar.scheduler

import com.example.investfeed.common.util.failure
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
                // 캐시가 있으면 동기화 없이 발표 감지만 (다운타임 중 발표 확인)
                log.debug { "CalendarSync 초기 동기화 생략 — Redis 캐시 있음, 발표 감지만 실행: ${detectReleases()}" }
                return@execute
            }
            syncCalendarData()
        }
    }

    @Scheduled(cron = SchedulerCron.CALENDAR_SYNC, scheduler = "slowScheduler")
    fun syncCalendarData() {
        schedulerLogService.execute(SchedulerName.CalendarSyncScheduler) {
            economicCalendarService.syncCurrentData()
            report(detectReleases())
        }
    }

    /** 발표 알림 감지. 실패해도 캘린더 동기화는 성공으로 두고 결과 문구만 돌려준다 */
    private fun detectReleases(): String =
        runCatching { releaseAlertService.run() }
            .map { r -> "발표 감지 scanned=${r.scanned} saved=${r.saved} suppressed=${r.suppressed} posted=${r.posted}${if (r.seed) " (seed)" else ""}" }
            .getOrElse { log.failure(it) { "발표 알림 감지 실패" }; "발표 감지 실패" }
}
