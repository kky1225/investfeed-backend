package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.calendar.repository.CalendarEventRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
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

    /**
     * 주식시장은 열고 미국 채권시장만 쉬는 날 (SIFMA 전면 마감 권고). 캘린더에는 표기하지 않고 브리핑 국채 저장·표시에만 쓴다.
     * 콜럼버스 데이(10월 둘째 월요일), 재향군인의 날(11/11, 일요일이면 다음 월요일).
     */
    fun isUsBondHoliday(date: LocalDate): Boolean {
        val columbusDay = LocalDate.of(date.year, 10, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY))
        val veteransDay = LocalDate.of(date.year, 11, 11).let { if (it.dayOfWeek == DayOfWeek.SUNDAY) it.plusDays(1) else it }
        return date == columbusDay || date == veteransDay
    }

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
