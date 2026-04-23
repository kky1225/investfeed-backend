package com.example.investfeed.global.holiday

import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.monitoring.service.SchedulerType
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class HolidayService(
    private val holidayClient: HolidayClient,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    private var holidays: Set<String> = emptySet()
    private var loadedMonth: String? = null

    @Scheduled(cron = "0 5 0 1 * *", scheduler = "slowScheduler")
    fun refreshHolidays() {
        schedulerLogService.execute("HolidayRefreshScheduler", SchedulerType.SLOW) {
            val now = LocalDate.now()
            loadHolidays(now.year, now.monthValue)
        }
    }

    fun isHoliday(): Boolean {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value

        if (dayOfWeek >= 6) return true

        val currentMonth = "${today.year}${String.format("%02d", today.monthValue)}"
        if (loadedMonth != currentMonth) {
            loadHolidays(today.year, today.monthValue)
        }

        return holidays.contains(today.format(formatter))
    }

    private fun loadHolidays(year: Int, month: Int) {
        val monthKey = "$year${String.format("%02d", month)}"
        try {
            val holidayDates = holidayClient.getHolidays(year, month)
            holidays = holidayDates.toSet()
            loadedMonth = monthKey
        } catch (e: Exception) {
            log.error(e) { "공휴일 목록 로드 실패: $monthKey" }
        }
    }
}
