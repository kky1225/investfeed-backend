package com.example.investfeed.global.holiday

import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service
class HolidayService(
    private val holidayClient: HolidayClient,
    private val marketHolidayRepository: MarketHolidayRepository,
    private val schedulerLogService: SchedulerLogService,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    companion object {
        const val MARKET_KR = "KR"
        const val SOURCE_API = "API"
    }

    private val monthlyHolidays: MutableMap<String, Set<String>> = ConcurrentHashMap()

    @Scheduled(cron = SchedulerCron.HOLIDAY_REFRESH, scheduler = "slowScheduler")
    fun refreshHolidays() {
        schedulerLogService.execute(SchedulerName.HolidayRefreshScheduler) {
            syncHolidays()
        }
    }

    @Order(0)
    @EventListener(ApplicationReadyEvent::class)
    fun initialLoad() {
        if (marketHolidayRepository.countByMarket(MARKET_KR) > 0L) return

        log.info { "공휴일 초기 적재 시작 — market_holiday 비어 있음" }
        runCatching {
            schedulerLogService.execute(SchedulerName.HolidayRefreshScheduler) {
                syncHolidays()
            }
        }.onFailure { log.error { "공휴일 초기 적재 실패: ${it.message}" } }
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

    private fun holidaysOf(year: Int, month: Int): Set<String> {
        val key = monthKey(year, month)
        return monthlyHolidays.getOrPut(key) {
            marketHolidayRepository
                .findAllByMarketAndDtBetween(MARKET_KR, "${key}01", "${key}31")
                .map { it.dt }
                .toSet()
        }
    }

    private fun syncHolidays() {
        val now = LocalDate.now()
        val years = now.year - 1..now.year + 1

        val fetched: Map<Int, Set<Pair<String, String>>> = years.associateWith { year ->
            (1..12).flatMap { month -> holidayClient.getHolidayInfos(year, month) }
                .map { it.date to it.name }
                .toSet()
        }

        for (year in now.year - 1..now.year) {
            if (fetched.getValue(year).isEmpty()) {
                throw IllegalStateException("공휴일 API 이상 응답: ${year}년 0건 — 동기화 중단")
            }
        }

        var inserted = 0
        var deleted = 0
        transactionTemplate.executeWithoutResult {
            for (year in years) {
                val apiSet = fetched.getValue(year)
                val dbRows = marketHolidayRepository
                    .findAllByMarketAndSourceAndDtBetween(MARKET_KR, SOURCE_API, "${year}0101", "${year}1231")
                val dbSet = dbRows.map { it.dt to it.name }.toSet()

                apiSet.filterNot { it in dbSet }.forEach { (dt, name) ->
                    marketHolidayRepository.save(
                        MarketHoliday(market = MARKET_KR, dt = dt, name = name, source = SOURCE_API)
                    )
                    inserted++
                }

                if (apiSet.isNotEmpty()) {
                    val stale = dbRows.filterNot { (it.dt to it.name) in apiSet }
                    stale.forEach { log.warn { "공휴일 삭제(공표 변경): ${it.dt} ${it.name}" } }
                    marketHolidayRepository.deleteAll(stale)
                    deleted += stale.size
                }
            }
        }

        monthlyHolidays.clear()
        log.info { "공휴일 동기화 완료: ${now.year - 1}~${now.year + 1}년, 신규 ${inserted}건, 삭제 ${deleted}건" }
    }

    private fun monthKey(year: Int, month: Int): String = "$year${String.format("%02d", month)}"
}
