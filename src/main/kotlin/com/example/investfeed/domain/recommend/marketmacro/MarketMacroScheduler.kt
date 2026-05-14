package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.domain.dashboard.DashboardIndexType
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class MarketMacroScheduler(
    private val sectClient: SectClient,
    private val marketMacroCacheService: MarketMacroCacheService,
    private val schedulerLogService: SchedulerLogService,
    private val holidayService: HolidayService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 * 9-15 * * MON-FRI", scheduler = "fastScheduler")
    fun pollDuringMarket() = run()

    @Scheduled(cron = "0 0 16 * * MON-FRI", scheduler = "fastScheduler")
    fun pollAtClose() = run()

    private fun run() {
        if (holidayService.isHoliday()) return
        schedulerLogService.execute(SchedulerName.MarketMacroScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doPoll()
            } catch (e: Exception) {
                log.error(e) { "MarketMacroScheduler 실패" }
                throw e
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
                val res = sectClient.sectInvestor(
                    req = KiwoomSectInvestorReq(
                        mrkt_tp = type.marketType,
                        amt_qty_tp = "0",
                        stex_tp = "3",
                    )
                )
                val first = res.inds_netprps?.firstOrNull() ?: return@runCatching
                val snapshot = MarketMacroSnapshot.from(type.name, first)
                marketMacroCacheService.saveSnapshot(snapshot)
            }.onFailure {
                log.error(it) { "MarketMacro polling failed: $type" }
            }
        }
    }
}
