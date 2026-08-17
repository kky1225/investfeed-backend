package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexInvestorDailyRepository
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class IndexInvestorDailyScheduler(
    private val indexService: IndexService,
    private val indexInvestorDailyRepository: IndexInvestorDailyRepository,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    private val holidayService: HolidayService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    companion object {
        private const val API_PACING_MS = 200L
    }

    @Scheduled(cron = SchedulerCron.INDEX_INVESTOR_DAILY, scheduler = "slowScheduler")
    fun collectDaily() {
        log.info { "IndexInvestorDailyScheduler cron fired" }
        schedulerLogService.execute(SchedulerName.IndexInvestorDailyScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                val filled = catchupMissing()
                log.info { "지수 투자자 일별 데이터 수집 완료: ${filled}일치" }
            } catch (e: Exception) {
                log.error { "지수 투자자 일별 데이터 수집 스케줄러 실패: ${e.message}" }
                throw e
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun fillMissingData() {
        if (schedulerLogService.isRunning(SchedulerName.IndexInvestorDailyScheduler)) {
            log.warn { "IndexInvestorDailyScheduler 기동 보정 skip: 이미 실행 중" }
            return
        }

        runCatching {
            schedulerLogService.execute(SchedulerName.IndexInvestorDailyScheduler) {
                setSchedulerSecurityContext()
                try {
                    authClient.accessToken()
                    val filled = catchupMissing()
                    log.info { "지수 투자자 일별 데이터 보정 완료: ${filled}일치 수집" }
                } finally {
                    SecurityContextHolder.clearContext()
                }
            }
        }.onFailure { log.error { "지수 투자자 일별 데이터 보정 실패: ${it.message}" } }
    }

    private fun catchupMissing(): Int {
        val lastDtKospi = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc("001")?.dt
        val lastDtKosdaq = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc("101")?.dt

        if (lastDtKospi == null && lastDtKosdaq == null) {
            backfillInitial(100)
            return 0
        }

        val lastDt = listOfNotNull(lastDtKospi, lastDtKosdaq).min()
        val lastDate = LocalDate.parse(lastDt, formatter)
        val yesterday = LocalDate.now().minusDays(1)

        if (!lastDate.isBefore(yesterday)) return 0

        var current = lastDate.plusDays(1)
        var filled = 0

        while (!current.isAfter(yesterday)) {
            if (!holidayService.isHoliday(current)) {
                val dt = current.format(formatter)
                val kospiMissing = !indexInvestorDailyRepository.existsByIndsCdAndDt("001", dt)
                val kosdaqMissing = !indexInvestorDailyRepository.existsByIndsCdAndDt("101", dt)
                if (kospiMissing || kosdaqMissing) {
                    try {
                        if (filled > 0) Thread.sleep(API_PACING_MS)
                        indexService.collectIndexInvestorDaily(dt)
                        filled++
                    } catch (e: Exception) {
                        log.warn { "지수 투자자 일별 데이터 보정 실패 ($dt): ${e.message}" }
                    }
                }
            }
            current = current.plusDays(1)
        }

        return filled
    }

    private fun backfillInitial(tradingDays: Int) {
        var current = LocalDate.now().minusDays(1)
        var collected = 0
        var failed = 0
        val maxScanDays = tradingDays * 2

        repeat(maxScanDays) {
            if (collected >= tradingDays) return@repeat
            if (!holidayService.isHoliday(current)) {
                val dt = current.format(formatter)
                try {
                    indexService.collectIndexInvestorDaily(dt)
                    collected++
                } catch (e: Exception) {
                    failed++
                    log.warn { "지수 투자자 일별 초기 시드 실패 ($dt): ${e.message}" }
                }
            }
            current = current.minusDays(1)
        }

        log.info { "지수 투자자 일별 초기 시드 완료: ${collected}일치 수집 (실패 ${failed}건)" }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
