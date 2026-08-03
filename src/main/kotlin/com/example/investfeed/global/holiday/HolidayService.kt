package com.example.investfeed.global.holiday

import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
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

    private val monthlyHolidays: MutableMap<String, Set<String>> = mutableMapOf()

    @Scheduled(cron = "0 5 0 1 * *", scheduler = "slowScheduler")
    fun refreshHolidays() {
        schedulerLogService.execute(SchedulerName.HolidayRefreshScheduler) {
            val now = LocalDate.now()
            loadHolidays(now.year, now.monthValue)
        }
    }

    fun isHoliday(): Boolean = isHoliday(LocalDate.now())

    fun isHoliday(date: LocalDate): Boolean {
        if (date.dayOfWeek.value >= 6) return true
        val holidays = holidaysOf(date.year, date.monthValue)
        return holidays.contains(date.format(formatter)) || isYearEndClosingDay(date)
    }

    private fun isYearEndClosingDay(date: LocalDate): Boolean {
        if (date.monthValue != 12) return false
        val holidays = holidaysOf(date.year, 12)
        var closure = LocalDate.of(date.year, 12, 31)
        while (closure.dayOfWeek.value >= 6 || holidays.contains(closure.format(formatter))) {
            closure = closure.minusDays(1)
        }
        return date == closure
    }

    fun nextTradingDay(from: LocalDate): LocalDate {
        var candidate = from.plusDays(1)
        repeat(60) {
            if (!isHoliday(candidate)) return candidate
            candidate = candidate.plusDays(1)
        }
        log.warn { "nextTradingDay: 60일 내 거래일을 찾지 못함 (from=$from) — 마지막 후보 반환: $candidate" }
        return candidate
    }

    fun lastTradingDay(from: LocalDate = LocalDate.now()): LocalDate {
        var candidate = from
        repeat(60) {
            if (!isHoliday(candidate)) return candidate
            candidate = candidate.minusDays(1)
        }
        log.warn { "lastTradingDay: 60일 내 거래일을 찾지 못함 (from=$from) — 마지막 후보 반환: $candidate" }
        return candidate
    }

    /** 해당 월의 공휴일 Set 를 반환. 미로드 시 client 호출 후 캐시. */
    private fun holidaysOf(year: Int, month: Int): Set<String> {
        val key = monthKey(year, month)
        monthlyHolidays[key]?.let { return it }
        loadHolidays(year, month)
        // 로드 실패 시 emptySet 으로 기본값 반환(반복 호출 방지)
        return monthlyHolidays[key] ?: emptySet()
    }

    private fun loadHolidays(year: Int, month: Int) {
        val key = monthKey(year, month)
        try {
            val holidayDates = holidayClient.getHolidays(year, month)
            monthlyHolidays[key] = holidayDates.toSet()
        } catch (e: Exception) {
            log.error(e) { "공휴일 목록 로드 실패: $key" }
            // 실패 시 빈 Set 으로 캐시해서 반복 호출 방지 (다음 월간 리프레시에서 재시도)
            monthlyHolidays[key] = emptySet()
        }
    }

    private fun monthKey(year: Int, month: Int): String = "$year${String.format("%02d", month)}"
}
