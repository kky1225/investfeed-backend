package com.example.investfeed.domain.assistant.scheduler

import com.example.investfeed.domain.assistant.service.IndexAlertService
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.KrSessionTaskScheduler
import com.example.investfeed.global.holiday.KrSessionTaskScheduler.WindowEnd
import com.example.investfeed.kiwoom.auth.service.AuthClient
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Component
class AssistantAlertScheduler(
    private val indexAlertService: IndexAlertService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val krSessionTaskScheduler: KrSessionTaskScheduler,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {

    companion object {
        val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
    }

    /** 국내: 거래일 정규장(평소 09:00~15:30, 수능일 10:00~16:30) 매분 :01 에만 예약된다. 마감 시각 회차 포함 */
    @PostConstruct
    fun registerKr() {
        krSessionTaskScheduler.registerDuringSession(
            SchedulerName.AssistantIndexAlertKrScheduler.name, Duration.ofMinutes(1), Duration.ofSeconds(1), WindowEnd.CLOSE_INCLUSIVE,
        ) { executeKr(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) }
    }

    @Scheduled(cron = SchedulerCron.ASSISTANT_INDEX_ALERT_US, scheduler = "fastScheduler")
    fun scheduledUs() = runUs()

    fun triggerKr() = executeKr(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
    fun triggerUs() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        executeUs(now, toNy(now).toLocalDate())
    }

    private fun executeKr(now: LocalDateTime) {
        schedulerLogService.execute(SchedulerName.AssistantIndexAlertKrScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                report(indexAlertService.summarize(indexAlertService.runKr(now)))
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun runUs() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        schedulerLogService.markFired(SchedulerName.AssistantIndexAlertUsScheduler)
        val ny = toNy(now)
        val usDate = ny.toLocalDate()
        val nyTime = ny.toLocalTime()
        if (!usMarketCalendarService.isUsTradingDay(usDate) || nyTime.isBefore(US_OPEN_ET) || nyTime.isAfter(usMarketCalendarService.closeTimeEt(usDate))) {
            schedulerLogService.skip(SchedulerName.AssistantIndexAlertUsScheduler, "미국 세션 밖 (ET $usDate $nyTime)")
            return
        }
        executeUs(now, usDate)
    }

    private fun toNy(now: LocalDateTime) =
        now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)

    private fun executeUs(now: LocalDateTime, usDate: LocalDate) {
        schedulerLogService.execute(SchedulerName.AssistantIndexAlertUsScheduler) {
            report(indexAlertService.summarize(indexAlertService.runUs(now, usDate)))
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
