package com.example.investfeed.domain.circuitbreaker.scheduler

import com.example.investfeed.domain.circuitbreaker.service.CircuitBreakerService
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.KrSessionTaskScheduler
import com.example.investfeed.global.holiday.KrSessionTaskScheduler.WindowEnd
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/** 정규장 중에만 예약된다 (평소 09:00~15:30, 수능일 10:00~16:30 — KrSessionTaskScheduler) */
@Component
class CircuitBreakerScheduler(
    private val circuitBreakerService: CircuitBreakerService,
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val krSessionTaskScheduler: KrSessionTaskScheduler,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {

    private val indexStream = StreamEntry(
        market = StreamMarket.KRX,
        items = CircuitBreakerService.INDS_CD.keys.toList(),
        types = listOf(CircuitBreakerService.REALTIME_TYPE),
    )

    @PostConstruct
    fun registerTasks() {
        // 매분 0J 상시 구독 보장 + 수신 간격 로그 (개장 :01 ~ 마감 전)
        krSessionTaskScheduler.registerDuringSession(
            SchedulerName.CircuitBreakerSubscribeScheduler.name, Duration.ofMinutes(1), Duration.ofSeconds(1), WindowEnd.CLOSE_EXCLUSIVE, ::subscribe,
        )
        // 매초 1분 유지 판정 (개장 ~ 마감 전)
        krSessionTaskScheduler.registerDuringSession(
            SchedulerName.CircuitBreakerCheckScheduler.name, Duration.ofSeconds(1), Duration.ZERO, WindowEnd.CLOSE_EXCLUSIVE, ::check,
        )
    }

    fun subscribe() {
        schedulerLogService.execute(SchedulerName.CircuitBreakerSubscribeScheduler) {
            kiwoomStreamClient.registerPersistent(schedulerLoginId, indexStream)
            val stats = circuitBreakerService.drainTickStats()
                .joinToString(", ") { "${it.indexCode} ${it.ticks}건 최대간격 ${it.maxGapSec}초" }
                .ifEmpty { "수신 없음" }
            report("0J $stats")
        }
    }

    // 매초 1분 유지 판정 (FAST: 성공은 scheduler_status 만, timeout 초과·예외만 scheduler_log). 발동 시 알림 게시는 IndexAlertService 리스너가 담당
    fun check() {
        schedulerLogService.execute(SchedulerName.CircuitBreakerCheckScheduler) {
            // 발동 시 알림 카드 수집이 키움 REST 를 호출하므로 스케줄러 계정으로 인증 컨텍스트를 둔다
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
            try {
                circuitBreakerService.check(LocalDateTime.now())
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }
}
