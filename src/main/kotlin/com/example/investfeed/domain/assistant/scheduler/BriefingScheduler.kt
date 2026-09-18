package com.example.investfeed.domain.assistant.scheduler

import com.example.investfeed.domain.assistant.service.BriefingService
import com.example.investfeed.domain.assistant.service.BriefingType
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class BriefingScheduler(
    private val briefingService: BriefingService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val US_CLOSE_DELAY_MINUTES = 10L
    }

    @Scheduled(cron = SchedulerCron.BRIEFING_KR_CLOSE, scheduler = "slowScheduler")
    fun scheduledKrClose() {
        val now = LocalDateTime.now()
        if (briefingService.resolveTargetDate(BriefingType.KR_CLOSE, now) != now.toLocalDate()) {
            schedulerLogService.markFired(SchedulerName.BriefingKrCloseScheduler)
            log.info { "BriefingKrCloseScheduler skipped: 오늘은 국내 거래일이 아님" }
            return
        }
        runWithSchedulerLog(SchedulerName.BriefingKrCloseScheduler, BriefingType.KR_CLOSE, now)
    }

    @Scheduled(cron = SchedulerCron.BRIEFING_KR_HOLDINGS, scheduler = "slowScheduler")
    fun scheduledKrHoldings() {
        val now = LocalDateTime.now()
        if (briefingService.resolveTargetDate(BriefingType.KR_HOLDINGS, now) != now.toLocalDate()) {
            schedulerLogService.markFired(SchedulerName.BriefingKrHoldingsScheduler)
            log.info { "BriefingKrHoldingsScheduler skipped: 오늘은 국내 거래일이 아님" }
            return
        }
        runWithSchedulerLog(SchedulerName.BriefingKrHoldingsScheduler, BriefingType.KR_HOLDINGS, now)
    }

    @Scheduled(cron = SchedulerCron.BRIEFING_KR_PRE, scheduler = "slowScheduler")
    fun scheduledKrPre() {
        val now = LocalDateTime.now()
        if (briefingService.resolveTargetDate(BriefingType.KR_PRE, now) != now.toLocalDate()) {
            schedulerLogService.markFired(SchedulerName.BriefingKrPreScheduler)
            log.info { "BriefingKrPreScheduler skipped: 오늘은 국내 거래일이 아님" }
            return
        }
        runWithSchedulerLog(SchedulerName.BriefingKrPreScheduler, BriefingType.KR_PRE, now)
    }

    @Scheduled(cron = SchedulerCron.BRIEFING_US_CLOSE, scheduler = "slowScheduler")
    fun scheduledUsClose() {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val target = usMarketCalendarService.lastClosedUsTradingDay(now)
        val due = usMarketCalendarService.closeKst(target).plusMinutes(US_CLOSE_DELAY_MINUTES).truncatedTo(ChronoUnit.MINUTES)
        if (now != due) return
        triggerUsClose()
    }

    @Scheduled(cron = SchedulerCron.BRIEFING_COIN_DAILY, scheduler = "slowScheduler")
    fun scheduledCoinDaily() = triggerCoinDaily()

    fun triggerKrClose() = runWithSchedulerLog(SchedulerName.BriefingKrCloseScheduler, BriefingType.KR_CLOSE)
    fun triggerKrHoldings() = runWithSchedulerLog(SchedulerName.BriefingKrHoldingsScheduler, BriefingType.KR_HOLDINGS)
    fun triggerCoinDaily() = runWithSchedulerLog(SchedulerName.BriefingCoinDailyScheduler, BriefingType.COIN_DAILY)
    fun triggerKrPre() = runWithSchedulerLog(SchedulerName.BriefingKrPreScheduler, BriefingType.KR_PRE)
    fun triggerUsClose() = runWithSchedulerLog(SchedulerName.BriefingUsCloseScheduler, BriefingType.US_CLOSE)

    private fun runWithSchedulerLog(name: SchedulerName, type: BriefingType, now: LocalDateTime = LocalDateTime.now()) {
        schedulerLogService.execute(name) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "스케줄러 키움 토큰 발급 실패 ($name)" }
                SecurityContextHolder.clearContext()
                throw e
            }
            try {
                val result = briefingService.generateAndPublish(type, now)
                log.info { "$name 완료: date=${result.targetDate} briefingId=${result.briefingId} posted=${result.posted} skipped=${result.skipped}" }
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
