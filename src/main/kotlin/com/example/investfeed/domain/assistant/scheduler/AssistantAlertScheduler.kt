package com.example.investfeed.domain.assistant.scheduler

import com.example.investfeed.domain.assistant.service.IndexAlertService
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Component
class AssistantAlertScheduler(
    private val indexAlertService: IndexAlertService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {

    companion object {
        val KR_OPEN: LocalTime = LocalTime.of(9, 0)
        val KR_CLOSE: LocalTime = LocalTime.of(15, 30)
        val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
    }

    @Scheduled(cron = SchedulerCron.ASSISTANT_INDEX_ALERT_KR, scheduler = "fastScheduler")
    fun scheduledKr() = runKr()

    @Scheduled(cron = SchedulerCron.ASSISTANT_INDEX_ALERT_US, scheduler = "fastScheduler")
    fun scheduledUs() = runUs()

    fun triggerKr() = executeKr(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
    fun triggerUs() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        executeUs(now, toNy(now).toLocalDate())
    }

    private fun runKr() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        schedulerLogService.markFired(SchedulerName.AssistantIndexAlertKrScheduler)
        if (holidayService.isHoliday(now.toLocalDate())) {
            schedulerLogService.skip(SchedulerName.AssistantIndexAlertKrScheduler, "휴일")
            return
        }
        val t = now.toLocalTime()
        if (t.isBefore(KR_OPEN) || t.isAfter(KR_CLOSE)) {
            schedulerLogService.skip(SchedulerName.AssistantIndexAlertKrScheduler, "국내 세션 밖 ($t)")
            return
        }
        executeKr(now)
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
