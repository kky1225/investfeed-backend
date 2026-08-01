package com.example.investfeed.domain.stock.scheduler

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.stock.repository.StockMasterRepository
import com.example.investfeed.domain.stock.service.StockMasterSyncService
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.domain.us.stock.service.UsStockInfoService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class StockMasterSyncScheduler(
    private val stockMasterSyncService: StockMasterSyncService,
    private val usStockInfoService: UsStockInfoService,
    private val stockMasterRepository: StockMasterRepository,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 5 * * *", scheduler = "slowScheduler")
    fun syncStockMaster() {
        schedulerLogService.execute(SchedulerName.StockMasterSyncScheduler) {
            sync(syncKr = true, syncUs = true)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialLoad() {
        val syncKr = stockMasterRepository.count() == 0L
        val syncUs = usStockMasterRepository.count() == 0L

        if (!syncKr && !syncUs) {
            return
        }

        log.info { "종목 마스터 초기 적재 시작 (국내=$syncKr, 미국=$syncUs)" }
        schedulerLogService.execute(SchedulerName.StockMasterSyncScheduler) {
            sync(syncKr = syncKr, syncUs = syncUs)
        }
    }

    private fun sync(syncKr: Boolean, syncUs: Boolean) {
        log.info { "종목 마스터 동기화 시작 (국내=$syncKr, 미국=$syncUs)" }
        val start = System.currentTimeMillis()

        setSchedulerSecurityContext()
        try {
            authClient.accessToken()
        } catch (e: Exception) {
            log.error(e) { "스케줄러 토큰 발급 실패" }
            SecurityContextHolder.clearContext()
            return
        }

        if (syncKr) {
            try {
                val count = stockMasterSyncService.syncAll()
                log.info { "국내 종목 마스터 적재 완료: ${count}건" }
            } catch (e: Exception) {
                log.error(e) { "국내 종목 마스터 적재 실패" }
            }
        }

        if (syncUs) {
            try {
                val count = usStockInfoService.syncAll()
                log.info { "미국 종목 마스터 적재 완료: ${count}건" }
            } catch (e: Exception) {
                log.error(e) { "미국 종목 마스터 적재 실패" }
            }
        }

        SecurityContextHolder.clearContext()
        log.info { "종목 마스터 동기화 완료: ${System.currentTimeMillis() - start}ms" }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
