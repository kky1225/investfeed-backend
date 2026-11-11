package com.example.investfeed.domain.index.scheduler

import com.example.investfeed.domain.index.repository.IndexDailyCloseRepository
import com.example.investfeed.domain.index.service.IndexService
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.global.holiday.HolidayService
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

    companion object {
        private const val KOSPI_CD = "001"
        private const val KOSDAQ_CD = "101"
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Scheduled(cron = SchedulerCron.INDEX_DAILY_CLOSE, scheduler = "slowScheduler")
    fun scheduledCollectIndexClose() {
        schedulerLogService.markFired(SchedulerName.IndexDailyCloseScheduler)
        val yesterday = LocalDate.now().minusDays(1)
        if (holidayService.isHoliday(yesterday)) {
            schedulerLogService.skip(SchedulerName.IndexDailyCloseScheduler, "전날($yesterday) 휴일")
            return
        }
        val yyyymmdd = yesterday.format(YYYYMMDD)
        val kospiExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSPI_CD, yyyymmdd)
        val kosdaqExists = indexDailyCloseRepository.existsByIndsCdAndDt(KOSDAQ_CD, yyyymmdd)
        if (kospiExists && kosdaqExists) {
            schedulerLogService.skip(SchedulerName.IndexDailyCloseScheduler, "전날($yesterday) 이미 수집됨")
            return
        }
        indexService.runCollectIndexClose()
    }
}
