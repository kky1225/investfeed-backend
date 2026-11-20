package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.domain.dashboard.DashboardIndexType
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.KrSessionTaskScheduler
import com.example.investfeed.global.holiday.KrSessionTaskScheduler.WindowEnd
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.common.util.failure
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import kotlinx.coroutines.runBlocking
import java.time.Duration

@Component
class MarketMacroScheduler(
    private val sectClient: SectClient,
    private val marketMacroCacheService: MarketMacroCacheService,
    private val schedulerLogService: SchedulerLogService,
    private val krSessionTaskScheduler: KrSessionTaskScheduler,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    /** 거래일 개장 ~ 마감+30분(평소 09:00~16:00, 수능일 10:00~17:00) 매분 :01 에만 예약된다. 마지막 회차가 마감 확정값 */
    @PostConstruct
    fun registerTasks() {
        krSessionTaskScheduler.registerDuringSession(
            SchedulerName.MarketMacroScheduler.name, Duration.ofMinutes(1), Duration.ofSeconds(1), WindowEnd.CLOSE_SETTLED_INCLUSIVE, ::pollDuringMarket,
        )
    }

    fun pollDuringMarket() {
        schedulerLogService.execute(SchedulerName.MarketMacroScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doPoll()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun doPoll() {
        listOf(DashboardIndexType.KOSPI, DashboardIndexType.KOSDAQ).forEach { type ->
            runCatching {
                val res = runBlocking { sectClient.sectInvestor(
                    req = KiwoomSectInvestorReq(
                        mrkt_tp = type.marketType,
                        amt_qty_tp = "0",
                        stex_tp = "3",
                    )
                ) }
                val first = res.inds_netprps?.firstOrNull() ?: return@runCatching
                val snapshot = MarketMacroSnapshot.from(type.name, first)
                marketMacroCacheService.saveSnapshot(snapshot)
            }.onFailure {
                log.failure(it) { "MarketMacro polling failed: $type" }
            }
        }
    }
}
