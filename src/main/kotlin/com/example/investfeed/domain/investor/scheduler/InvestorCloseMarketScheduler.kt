package com.example.investfeed.domain.investor.scheduler

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.domain.investor.service.InvestorService
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
import java.time.LocalTime

/**
 * 투자자별 장마감 순위(ka10066) 데이터를 매분 백그라운드로 Redis 에 갱신한다.
 *
 * - 실행 구간: 평일 15:36 ~ 21:00
 *   · 15:36 (KRX_TRADE_CLOSE) 부터 장마감 데이터 집계 시작
 *   · 20:00 NXT 마감 이후 데이터 확정, 21:00 까지는 정정거래 대비 여유
 * - 휴일(주말/공휴일) skip
 * - 실행 결과가 없어도 사용자 요청은 InvestorService 의 on-demand fallback 으로 처리됨
 */
@Component
class InvestorCloseMarketScheduler(
    private val investorService: InvestorService,
    private val holidayService: HolidayService,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val RUN_START: LocalTime = MarketTimeUtil.KRX_TRADE_CLOSE  // 15:36
        private val RUN_END: LocalTime = LocalTime.of(21, 0)
    }

    @Scheduled(cron = "0 * * * * *", scheduler = "fastScheduler")
    fun refresh() {
        schedulerLogService.execute(SchedulerName.InvestorCloseMarketScheduler) {
            if (holidayService.isHoliday()) return@execute

            val now = LocalTime.now()
            if (now.isBefore(RUN_START) || !now.isBefore(RUN_END)) return@execute

            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "InvestorCloseMarketScheduler 토큰 발급 실패" }
                SecurityContextHolder.clearContext()
                return@execute
            }

            try {
                investorService.refreshCloseMarketCache(now)
            } catch (e: Exception) {
                log.error(e) { "InvestorCloseMarketScheduler 캐시 갱신 실패" }
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
