package com.example.investfeed.domain.watchlist.scheduler

import com.example.investfeed.common.util.failure
import com.example.investfeed.domain.holding.service.MemberHoldingSyncService
import com.example.investfeed.domain.watchlist.service.WatchlistSyncService
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

@Component
class WatchlistSyncScheduler(
    private val watchlistSyncService: WatchlistSyncService,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.WATCHLIST_SYNC, scheduler = "slowScheduler")
    fun syncAllStkNm() {
        schedulerLogService.execute(SchedulerName.WatchlistSyncScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.failure(e) { "스케줄러 토큰 발급 실패" }
                report("토큰 발급 실패로 중단")
                SecurityContextHolder.clearContext()
                return@execute
            }

            try {
                report(watchlistSyncService.syncAllStkNm())
            } catch (e: Exception) {
                log.failure(e) { "관심종목 stkNm 동기화 실패" }
                report("관심종목 실패")
            }

            try {
                report(memberHoldingSyncService.syncAllManualStkNm())
            } catch (e: Exception) {
                log.failure(e) { "수동 보유종목 stkNm 동기화 실패" }
                report("수동 보유 실패")
            }

            SecurityContextHolder.clearContext()
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
