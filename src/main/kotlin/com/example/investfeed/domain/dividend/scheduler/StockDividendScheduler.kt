package com.example.investfeed.domain.dividend.scheduler

import com.example.investfeed.domain.dividend.service.StockDividendService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockDividendScheduler(
    private val stockDividendService: StockDividendService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.STOCK_DIVIDEND, scheduler = "slowScheduler")
    fun collectDaily() {
        schedulerLogService.execute(SchedulerName.StockDividendScheduler) {
            try {
                stockDividendService.collectDailyDividends()
                log.info { "배당 정보 일별 수집 스케줄러 완료" }
            } catch (e: Exception) {
                log.error { "배당 정보 일별 수집 스케줄러 실패: ${e.message}" }
                throw e
            }
        }
    }
}
