package com.example.investfeed.domain.marketindex.scheduler

import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MarketIndexScheduler(
    private val crawler: NaverMarketIndexCrawler,
    private val marketIndexService: MarketIndexService,
    private val schedulerLogService: SchedulerLogService,
) {

    @Scheduled(cron = SchedulerCron.MARKET_INDEX, scheduler = "fastScheduler")
    fun crawlAndSave() {
        schedulerLogService.execute(SchedulerName.MarketIndexScheduler) {
            val list = crawler.crawl()

            if (list.isEmpty()) {
                return@execute
            }

            marketIndexService.saveAll(list)
        }
    }
}
