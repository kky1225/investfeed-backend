package com.example.investfeed.domain.calendar.scheduler

import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.monitoring.service.SchedulerType
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component

/**
 * 일정(경제지표) 데이터를 30분 주기로 외부 API 에서 가져와 Redis 에 갱신한다.
 *
 * 기동 시 초기 warming 은 `ApplicationReadyEvent` 수신 후 slowScheduler 풀에 제출한다.
 * - 스레드 누수/추적 가능성 확보 (슬로우 풀 지표에 노출)
 */
@Component
class CalendarSyncScheduler(
    private val economicCalendarService: EconomicCalendarService,
    private val schedulerLogService: SchedulerLogService,
    @Qualifier("slowScheduler") private val slowScheduler: ThreadPoolTaskScheduler,
) {
    private val log = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun initSync() {
        slowScheduler.execute {
            if (economicCalendarService.isCacheWarm()) {
                log.info { "CalendarSync 초기 warming skip — Redis 캐시 이미 존재" }
                return@execute
            }
            syncCalendarData()
        }
    }

    @Scheduled(cron = "0 */30 * * * *", scheduler = "slowScheduler")
    fun syncCalendarData() {
        schedulerLogService.execute("CalendarSyncScheduler", SchedulerType.SLOW) {
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
    }
}
