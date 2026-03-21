package com.example.investfeed.domain.marketindex.scheduler

import com.example.investfeed.domain.marketindex.crawler.NaverMarketIndexCrawler
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MarketIndexScheduler(
    private val crawler: NaverMarketIndexCrawler,
    private val marketIndexService: MarketIndexService,
) {

    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 * * * * *")
    fun crawlAndSave() {
        try {
            val list = crawler.crawl()

            if (list.isEmpty()) {
                return
            }

            marketIndexService.saveAll(list)
        } catch (e: Exception) {
            log.error(e) { "시장지표 크롤링 실패" }
        }
    }
}
