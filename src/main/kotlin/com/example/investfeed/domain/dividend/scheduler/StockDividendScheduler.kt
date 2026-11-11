package com.example.investfeed.domain.dividend.scheduler

import com.example.investfeed.domain.dividend.service.StockDividendService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockDividendScheduler(
    private val stockDividendService: StockDividendService,
    private val schedulerLogService: SchedulerLogService,
) {

    @Scheduled(cron = SchedulerCron.STOCK_DIVIDEND, scheduler = "slowScheduler")
    fun collectDaily() {
        schedulerLogService.execute(SchedulerName.StockDividendScheduler) {
            report(stockDividendService.collectDailyDividends())
        }
    }
}
