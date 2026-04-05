package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.service.IndexService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class IndexInvestorDailyScheduler(
    private val indexService: IndexService
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 7 * * *")
    fun collectDaily() {
        try {
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            indexService.collectIndexInvestorDaily(yesterday)
            log.info { "지수 투자자 일별 데이터 수집 완료: $yesterday" }
        } catch (e: Exception) {
            log.error { "지수 투자자 일별 데이터 수집 스케줄러 실패: ${e.message}" }
        }
    }
}
