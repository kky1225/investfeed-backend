package com.example.investfeed.global.holiday

import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 날짜별 국내 장 시각. 평소 09:00~15:30, 평소와 다른 날은 두 가지:
 * - 관리자가 등록한 날 (market_session_override — 수능일 등)
 * - 새해 첫 거래일: 개장식으로 10:00 개장·15:30 마감·NXT 프리마켓 휴장 (2025·2026년 거래소 공지 동일). 등록한 값이 있으면 그 값이 우선
 */
@Service
class KrMarketSessionService(
    private val repository: MarketSessionOverrideRepository,
    private val holidayService: HolidayService,
) {
    companion object {
        const val NEW_YEAR_NAME = "새해 첫 거래일"
        val NEW_YEAR_OPEN: LocalTime = LocalTime.of(10, 0)
        /**
         * 등록 가능한 장 시각 범위 — 잘못 입력(오타 등)을 막는 안전 범위.
         * 예약(KrSessionTaskScheduler)은 장 시각을 그대로 따라가 cron 시간대 제약은 없다.
         * 실제 공지 사례(개장 10:00, 마감 15:30·16:30)를 넉넉히 덮는다
         */
        val OPEN_RANGE: ClosedRange<LocalTime> = LocalTime.of(9, 0)..LocalTime.of(11, 0)
        val CLOSE_RANGE: ClosedRange<LocalTime> = LocalTime.of(15, 0)..LocalTime.of(16, 59)
    }

    private val yearlyOverrides: MutableMap<Int, Map<LocalDate, MarketSessionOverride>> = ConcurrentHashMap()

    fun today(): KrSession = of(LocalDate.now())

    fun of(date: LocalDate): KrSession {
        overridesOf(date.year)[date]?.let {
            return KrSession(date, it.openTime, it.closeTime, it.preMarket, it.name)
        }
        if (isNewYearFirstTradingDay(date)) {
            return KrSession(date, NEW_YEAR_OPEN, KrSession.DEFAULT_CLOSE, preMarket = false, name = NEW_YEAR_NAME)
        }
        return KrSession.default(date)
    }

    /** 그 달의 평소와 다른 날 (캘린더 표시용). 휴장일은 빼고 날짜순 */
    fun specialDaysOf(year: Int, month: Int): List<KrSession> {
        val registered = overridesOf(year).keys.filter { it.monthValue == month }
        val newYear = if (month == 1) listOf(firstTradingDayOf(year)) else emptyList()
        return (registered + newYear).distinct()
            .filterNot { holidayService.isHoliday(it) }
            .map(::of)
            .filter { it.isSpecial }
            .sortedBy { it.date }
    }

    /** 직전에 마감 데이터 정리까지 끝난 거래일 (마감 +30분 기준 — 평소 16:00, 수능일 17:00) */
    fun lastClosedTradingDay(now: LocalDateTime = LocalDateTime.now()): LocalDate {
        val today = now.toLocalDate()
        val settled = !holidayService.isHoliday(today) && !now.toLocalTime().isBefore(of(today).closeSettled)
        return if (settled) today else holidayService.lastTradingDay(today.minusDays(1))
    }

    /**
     * 평소 시각(normalCron)에 도는 작업의 그날 실행 시각 — 개장 또는 마감이 밀린 만큼 늦춘다 (KrSessionTaskScheduler 가 예약에 사용).
     * 평소 cron 이 그날 돌지 않으면 null
     */
    fun dueAt(date: LocalDate, normalCron: String, anchor: KrSessionAnchor): LocalDateTime? {
        val normal = CronExpression.parse(normalCron).next(date.atStartOfDay().minusNanos(1)) ?: return null
        if (normal.toLocalDate() != date) return null
        val session = of(date)
        return normal.plus(if (anchor == KrSessionAnchor.OPEN) session.openShift else session.closeShift)
    }

    /** 관리자 등록·수정·삭제 후 호출 — 다음 조회부터 바로 반영 */
    fun evict() = yearlyOverrides.clear()

    private fun isNewYearFirstTradingDay(date: LocalDate): Boolean =
        date.monthValue == 1 && !holidayService.isHoliday(date) && date == firstTradingDayOf(date.year)

    private fun firstTradingDayOf(year: Int): LocalDate = holidayService.nextTradingDay(LocalDate.of(year - 1, 12, 31))

    private fun overridesOf(year: Int): Map<LocalDate, MarketSessionOverride> =
        yearlyOverrides.getOrPut(year) {
            repository.findAllByMarketAndTradeDateBetweenOrderByTradeDate(
                HolidayService.MARKET_KR, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
            ).associateBy { it.tradeDate }
        }
}
