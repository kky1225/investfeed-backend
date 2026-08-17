package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexDailyCloseRepository
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class IndexDailyCloseScheduler(
    private val indexService: IndexService,
    private val holidayService: HolidayService,
    private val indexDailyCloseRepository: IndexDailyCloseRepository,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val KOSPI_CD = "001"
        private const val KOSDAQ_CD = "101"
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Scheduled(cron = SchedulerCron.INDEX_DAILY_CLOSE, scheduler = "slowScheduler")
    fun scheduledCollectIndexClose() {
        log.info { "IndexDailyCloseScheduler cron fired" }
        schedulerLogService.markFired(SchedulerName.IndexDailyCloseScheduler)
        val yesterday = LocalDate.now().minusDays(1)
        if (holidayService.isHoliday(yesterday)) {
            log.info { "IndexDailyCloseScheduler skipped: yesterday($yesterday) is holiday" }
            return
        }
        val yyyymmdd = yesterday.format(YYYYMMDD)
        val kospiExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSPI_CD, yyyymmdd)
        val kosdaqExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSDAQ_CD, yyyymmdd)
        if (kospiExists && kosdaqExists) {
            log.info { "IndexDailyCloseScheduler skipped: yesterday($yesterday) data already collected" }
            return
        }
        indexService.runCollectIndexClose()
    }
}
