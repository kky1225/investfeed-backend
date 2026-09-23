package com.example.investfeed.domain.assistant.scheduler

import com.example.investfeed.domain.assistant.service.IndexAlertService
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
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
    private val log = KotlinLogging.logger {}

    companion object {
        val KR_OPEN: LocalTime = LocalTime.of(9, 0)
        val KR_CLOSE: LocalTime = LocalTime.of(15, 30)
        val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
    }

    @Scheduled(cron = SchedulerCron.ASSISTANT_INDEX_ALERT_KR, scheduler = "fastScheduler")
    fun scheduledKr() = runKr()

    @Scheduled(cron = SchedulerCron.ASSISTANT_INDEX_ALERT_US, scheduler = "fastScheduler")
    fun scheduledUs() = runUs()

    fun triggerKr() = runKr()
    fun triggerUs() = runUs()

    private fun runKr() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        schedulerLogService.markFired(SchedulerName.AssistantIndexAlertKrScheduler)
        if (holidayService.isHoliday(now.toLocalDate())) return
        val t = now.toLocalTime()
        if (t.isBefore(KR_OPEN) || t.isAfter(KR_CLOSE)) {
            log.info { "AssistantIndexAlertKrScheduler skipped: 국내 세션 밖 ($t)" }
            return
        }
        schedulerLogService.execute(SchedulerName.AssistantIndexAlertKrScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                val result = indexAlertService.runKr(now)
                log.info { "AssistantIndexAlertKrScheduler ${indexAlertService.summarize(result)}" }
            } catch (e: Exception) {
                log.error(e) { "AssistantIndexAlertKrScheduler 실패" }
                throw e
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun runUs() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        schedulerLogService.markFired(SchedulerName.AssistantIndexAlertUsScheduler)
        val ny = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val usDate = ny.toLocalDate()
        val nyTime = ny.toLocalTime()
        if (!usMarketCalendarService.isUsTradingDay(usDate) || nyTime.isBefore(US_OPEN_ET) || nyTime.isAfter(usMarketCalendarService.closeTimeEt(usDate))) {
            log.info { "AssistantIndexAlertUsScheduler skipped: 미국 세션 밖 (ET $usDate $nyTime)" }
            return
        }
        schedulerLogService.execute(SchedulerName.AssistantIndexAlertUsScheduler) {
            try {
                val result = indexAlertService.runUs(now, usDate)
                log.info { "AssistantIndexAlertUsScheduler ${indexAlertService.summarize(result)}" }
            } catch (e: Exception) {
                log.error(e) { "AssistantIndexAlertUsScheduler 실패" }
                throw e
            }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
