package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.global.holiday.HolidayService
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Service
class PeriodResolver(private val holidayService: HolidayService) {

    enum class Period { THIS_MONTH, LAST_MONTH, THIS_YEAR }

    fun resolve(period: Period, today: LocalDate = LocalDate.now()): Pair<Int, Int?> = when (period) {
        Period.THIS_MONTH -> today.year to today.monthValue
        Period.LAST_MONTH -> YearMonth.from(today).minusMonths(1).let { it.year to it.monthValue }
        Period.THIS_YEAR -> today.year to null
    }

    fun lastTradingDay(from: LocalDate = LocalDate.now()): LocalDate {
        var d = from
        while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY || holidayService.isHoliday(d)) d = d.minusDays(1)
        return d
    }

    fun tradingDaysBack(n: Int, end: LocalDate = lastTradingDay()): LocalDate {
        var d = end
        var count = 1
        while (count < n) {
            d = lastTradingDay(d.minusDays(1))
            count++
        }
        return d
    }
}
