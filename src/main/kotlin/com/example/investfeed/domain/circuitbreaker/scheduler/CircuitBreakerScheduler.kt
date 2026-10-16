package com.example.investfeed.domain.circuitbreaker.scheduler

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.circuitbreaker.service.CircuitBreakerService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CircuitBreakerScheduler(
    private val circuitBreakerService: CircuitBreakerService,
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    private val indexStream = StreamEntry(
        market = StreamMarket.KRX,
        items = CircuitBreakerService.INDS_CD.keys.toList(),
        types = listOf(CircuitBreakerService.REALTIME_TYPE),
    )

    @Scheduled(cron = SchedulerCron.CIRCUIT_BREAKER_SUBSCRIBE, scheduler = "fastScheduler")
    fun subscribe() {
        schedulerLogService.markFired(SchedulerName.CircuitBreakerSubscribeScheduler)
        if (holidayService.isHoliday() || !MarketTimeUtil.isKrxOpen()) return

        schedulerLogService.execute(SchedulerName.CircuitBreakerSubscribeScheduler) {
            try {
                kiwoomStreamClient.registerPersistent(schedulerLoginId, indexStream)
                val stats = circuitBreakerService.drainTickStats()
                    .joinToString(", ") { "${it.indexCode} ${it.ticks}건 최대간격 ${it.maxGapSec}초" }
                    .ifEmpty { "수신 없음" }
                log.info { "CircuitBreakerSubscribeScheduler 0J $stats" }
            } catch (e: Exception) {
                log.error(e) { "CircuitBreakerSubscribeScheduler 실패" }
                throw e
            }
        }
    }

    // 매초 1분 유지 판정 (FAST: 성공은 scheduler_status 만, timeout 초과·예외만 scheduler_log). 발동 시 알림 게시는 IndexAlertService 리스너가 담당
    @Scheduled(cron = SchedulerCron.CIRCUIT_BREAKER_CHECK, scheduler = "fastScheduler")
    fun check() {
        schedulerLogService.markFired(SchedulerName.CircuitBreakerCheckScheduler)
        if (holidayService.isHoliday() || !MarketTimeUtil.isKrxOpen()) return

        schedulerLogService.execute(SchedulerName.CircuitBreakerCheckScheduler) {
            // 발동 시 알림 카드 수집이 키움 REST 를 호출하므로 스케줄러 계정으로 인증 컨텍스트를 둔다
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
            try {
                circuitBreakerService.check(LocalDateTime.now())
            } catch (e: Exception) {
                log.error(e) { "CircuitBreakerCheckScheduler 실패" }
                throw e
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }
}
