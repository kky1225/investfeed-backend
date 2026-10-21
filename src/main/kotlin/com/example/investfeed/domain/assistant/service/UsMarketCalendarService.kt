package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.calendar.repository.CalendarEventRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val PRE_OPEN_MIN = 4 * 60          // 프리마켓 04:00 ET
        private const val REGULAR_OPEN_MIN = 9 * 60 + 30 // 정규장 09:30 ET
        private const val DAY_MARKET_MIN = 20 * 60       // 애프터마켓 끝(20:00 ET)부터 주간거래
    }

    enum class UsSession(val label: String) { DAY("데이마켓"), PRE("프리마켓"), REGULAR("정규장"), AFTER("애프터마켓") }

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

    /**
     * 키움 미국 체결 시각(cntr_tm yyyyMMddHHmmss, bus_dt yyyyMMdd)으로 장구분·현지 거래일을 정해 카드 기준 문구를 만든다. 예: "10/19(월) 데이마켓". 시각은 표기하지 않는다 (2026-10-20, 장구분만).
     * 키움은 주간거래를 직전 영업일에 이어 24시를 넘긴 시각으로 준다 → 20:00 이후(24시 넘김 포함)는 다음 미국 거래일의 주간거래.
     * 시각을 못 읽으면 null (호출부가 달력 기준 문구로 대신한다)
     */
    fun tickBasis(cntrTm: String?, busDt: String?): String? =
        tickSession(cntrTm, busDt)?.let { (date, session) -> "${TemplateFormat.monthDay(date)} ${session.label}" }

    /** 최근 체결이 속한 현지 거래일과 장구분 (데이마켓은 다음 미국 거래일). 시각을 못 읽으면 null */
    fun tickSession(cntrTm: String?, busDt: String?): Pair<LocalDate, UsSession>? {
        val t = cntrTm?.takeIf { it.length >= 12 } ?: return null
        val bus = runCatching { LocalDate.parse(busDt?.takeIf { it.length == 8 } ?: t.take(8), YYYYMMDD) }.getOrNull() ?: return null
        val hh = t.substring(8, 10).toIntOrNull() ?: return null
        val mm = t.substring(10, 12).toIntOrNull() ?: return null
        val minutes = hh * 60 + mm
        val close = closeTimeEt(bus).let { it.hour * 60 + it.minute }
        return when {
            minutes >= DAY_MARKET_MIN -> nextUsTradingDay(bus) to UsSession.DAY
            minutes >= close -> bus to UsSession.AFTER
            minutes >= REGULAR_OPEN_MIN -> bus to UsSession.REGULAR
            minutes >= PRE_OPEN_MIN -> bus to UsSession.PRE
            else -> bus to UsSession.DAY   // 24시를 넘기지 않고 자정 이후로 오면 영업일이 이미 넘어간 것으로 본다 (실측 사례 없음)
        }
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
