package com.example.investfeed.domain.calendar.scheduler

import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 일정(경제지표) 데이터를 30분 주기로 외부 API 에서 가져와 Redis 에 갱신한다.
 *
 * 사용자가 일정 페이지를 조회할 때 외부 API 호출 없이 Redis 에서 즉시 응답할 수 있도록
 * 백그라운드에서 캐시를 항상 warm 상태로 유지한다.
 *
 * 서버 기동 시 @PostConstruct 로 즉시 1회 실행하여 Redis 가 빈 상태로
 * 사용자 요청을 받는 상황을 방지한다.
 */
@Component
class CalendarSyncScheduler(
    private val economicCalendarService: EconomicCalendarService,
) {
    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun initSync() {
        Thread {
            syncCalendarData()
        }.start()
    }

    @Scheduled(cron = "0 */30 * * * *", scheduler = "slowScheduler")
    fun syncCalendarData() {
        val start = System.currentTimeMillis()
        try {
            economicCalendarService.syncCurrentData()
        } catch (e: Exception) {
            log.error { "CalendarSyncScheduler 실행 실패: ${e.message}" }
        } finally {
            log.info { "CalendarSyncScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }
}
