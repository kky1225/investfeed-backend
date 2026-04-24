package com.example.investfeed.domain.interest.scheduler

import com.example.investfeed.domain.holding.service.MemberHoldingSyncService
import com.example.investfeed.domain.interest.service.InterestSyncService
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.monitoring.service.SchedulerType
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 관심종목 + 수동 보유종목의 stkNm 을 키움 API 응답값과 동기화한다.
 *
 * - cron: 매일 새벽 5시 15분 (사용자 활동 적은 시간, 다른 슬로우 스케줄러와 충돌 회피, 장 시작 8시 전)
 * - 자동 키움 보유종목은 HoldingSyncScheduler 가 처리하므로 제외
 * - 회사명 변경(예: LIG넥스원 → LIG디펜스앤에어로스페이스)을 일 1회 반영
 */
@Component
class InterestSyncScheduler(
    private val interestSyncService: InterestSyncService,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 15 5 * * *", scheduler = "slowScheduler")
    fun syncAllStkNm() {
        schedulerLogService.execute("InterestSyncScheduler", SchedulerType.SLOW) {
            log.info { "관심종목/수동 보유종목 stkNm 동기화 스케줄러 시작" }
            val start = System.currentTimeMillis()

            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "스케줄러 토큰 발급 실패" }
                SecurityContextHolder.clearContext()
                return@execute
            }

            try {
                interestSyncService.syncAllStkNm()
            } catch (e: Exception) {
                log.error(e) { "관심종목 stkNm 동기화 실패" }
            }

            try {
                memberHoldingSyncService.syncAllManualStkNm()
            } catch (e: Exception) {
                log.error(e) { "수동 보유종목 stkNm 동기화 실패" }
            }

            SecurityContextHolder.clearContext()
            log.info { "관심종목/수동 보유종목 stkNm 동기화 스케줄러 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
