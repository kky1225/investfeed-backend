package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexInvestorDailyRepository
import com.example.investfeed.domain.index.service.IndexService
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
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    @Scheduled(cron = "0 0 7 * * *", scheduler = "slowScheduler")
    fun collectDaily() {
        try {
            val yesterday = LocalDate.now().minusDays(1).format(formatter)
            indexService.collectIndexInvestorDaily(yesterday)
            log.info { "지수 투자자 일별 데이터 수집 완료: $yesterday" }
        } catch (e: Exception) {
            log.error { "지수 투자자 일별 데이터 수집 스케줄러 실패: ${e.message}" }
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

            if (lastDtKospi == null && lastDtKosdaq == null) return

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
                            log.error { "지수 투자자 일별 데이터 보정 실패 ($dt): ${e.message}" }
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
}
