package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.calendar.repository.CalendarEventRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class UsMarketCalendarService(
    private val calendarEventRepository: CalendarEventRepository,
) {
    companion object {
        val NY: ZoneId = ZoneId.of("America/New_York")
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        val REGULAR_CLOSE_ET: LocalTime = LocalTime.of(16, 0)
        val EARLY_CLOSE_ET: LocalTime = LocalTime.of(13, 0)
        const val EARLY_CLOSE_KEYWORD = "조기폐장"
    }

    private data class YearCalendar(val fullHolidays: Set<LocalDate>, val earlyCloses: Set<LocalDate>)
    private val cache = ConcurrentHashMap<Int, YearCalendar>()

    fun isUsTradingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY && date !in yearOf(date.year).fullHolidays

    fun isEarlyClose(date: LocalDate): Boolean = date in yearOf(date.year).earlyCloses

    fun closeTimeEt(date: LocalDate): LocalTime = if (isEarlyClose(date)) EARLY_CLOSE_ET else REGULAR_CLOSE_ET

    fun closeKst(tradeDate: LocalDate): LocalDateTime =
        ZonedDateTime.of(tradeDate, closeTimeEt(tradeDate), NY).withZoneSameInstant(KST).toLocalDateTime()

    fun lastClosedUsTradingDay(nowKst: LocalDateTime = LocalDateTime.now(KST)): LocalDate {
        var candidate = nowKst.atZone(KST).withZoneSameInstant(NY).toLocalDate()
        repeat(10) {
            if (isUsTradingDay(candidate) && !closeKst(candidate).isAfter(nowKst)) return candidate
            candidate = candidate.minusDays(1)
        }
        return candidate
    }

    fun nextUsTradingDay(from: LocalDate): LocalDate {
        var candidate = from.plusDays(1)
        repeat(10) {
            if (isUsTradingDay(candidate)) return candidate
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun yearOf(year: Int): YearCalendar = cache.computeIfAbsent(year) {
        val rows = calendarEventRepository.findByCountryAndTypeAndEventDateBetween(
            "US", "HOLIDAY", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)
        )
        val (early, full) = rows.partition { it.name.contains(EARLY_CLOSE_KEYWORD) }
        YearCalendar(
            fullHolidays = full.map { it.eventDate }.toSet(),
            earlyCloses = early.map { it.eventDate }.toSet(),
        )
    }
}
