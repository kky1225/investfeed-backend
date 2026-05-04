package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexInvestorDailyRepository
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class IndexInvestorDailyScheduler(
    private val indexService: IndexService,
    private val indexInvestorDailyRepository: IndexInvestorDailyRepository,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    @Scheduled(cron = "0 0 7 * * *", scheduler = "slowScheduler")
    fun collectDaily() {
        schedulerLogService.execute(SchedulerName.IndexInvestorDailyScheduler) {
            try {
                val yesterday = LocalDate.now().minusDays(1).format(formatter)
                indexService.collectIndexInvestorDaily(yesterday)
                log.info { "지수 투자자 일별 데이터 수집 완료: $yesterday" }
            } catch (e: Exception) {
                log.error { "지수 투자자 일별 데이터 수집 스케줄러 실패: ${e.message}" }
                throw e
            }
        }
    }

    // 서버 시작 시 누락 데이터 자동 보정 (로컬 환경용, AWS 배포 후 제거 예정)
    @EventListener(ApplicationReadyEvent::class)
    fun fillMissingData() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
        try {
            authClient.accessToken()
            // 코스피/코스닥 중 더 오래된 날짜 기준
            val lastKospi = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc("001")
            val lastKosdaq = indexInvestorDailyRepository.findFirstByIndsCdOrderByDtDesc("101")
            val lastDtKospi = lastKospi?.dt
            val lastDtKosdaq = lastKosdaq?.dt

            // 빈 테이블 — 어제 기준으로 과거 100 평일치 초기 시드
            if (lastDtKospi == null && lastDtKosdaq == null) {
                backfillInitial(100)
                return
            }

            val lastDt = listOfNotNull(lastDtKospi, lastDtKosdaq).min()
            val lastDate = LocalDate.parse(lastDt, formatter)
            val yesterday = LocalDate.now().minusDays(1)

            if (!lastDate.isBefore(yesterday)) return

            var current = lastDate.plusDays(1)
            var filled = 0

            while (!current.isAfter(yesterday)) {
                if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
                    val dt = current.format(formatter)
                    val kospiMissing = !indexInvestorDailyRepository.existsByIndsCdAndDt("001", dt)
                    val kosdaqMissing = !indexInvestorDailyRepository.existsByIndsCdAndDt("101", dt)
                    if (kospiMissing || kosdaqMissing) {
                        try {
                            indexService.collectIndexInvestorDaily(dt)
                            filled++
                        } catch (e: Exception) {
                            log.warn { "지수 투자자 일별 데이터 보정 실패 ($dt): ${e.message}" }
                        }
                    }
                }
                current = current.plusDays(1)
            }

            if (filled > 0) {
                log.info { "지수 투자자 일별 데이터 보정 완료: ${filled}일치 수집" }
            }
        } catch (e: Exception) {
            log.error { "지수 투자자 일별 데이터 보정 실패: ${e.message}" }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    /** 빈 테이블 초기 시드 — 어제부터 과거로 거슬러 올라가며 평일 N일치 수집. */
    private fun backfillInitial(weekdays: Int) {
        var current = LocalDate.now().minusDays(1)
        var collected = 0
        var failed = 0
        // safety: 최대 검사 calendar 일수 제한 (평일 N일이면 최대 N*2 calendar)
        val maxScanDays = weekdays * 2

        repeat(maxScanDays) {
            if (collected >= weekdays) return@repeat
            if (current.dayOfWeek != DayOfWeek.SATURDAY && current.dayOfWeek != DayOfWeek.SUNDAY) {
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
}
