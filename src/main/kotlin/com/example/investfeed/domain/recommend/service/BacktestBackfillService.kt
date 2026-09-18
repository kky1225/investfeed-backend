package com.example.investfeed.domain.recommend.service

import com.example.investfeed.kiwoom.chart.dto.stock.res.KiwoomStockChartDay
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.recommend.entity.StockPickHistory
import com.example.investfeed.domain.recommend.repository.StockPickHistoryRepository
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.chart.client.StockChartClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartDayReq
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import com.example.investfeed.global.coroutine.mapConcurrently

@Service
class BacktestBackfillService(
    private val stockChartClient: StockChartClient,
    private val stockPickHistoryRepository: StockPickHistoryRepository,
    private val holidayService: HolidayService,
    private val schedulerLogService: SchedulerLogService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val MARKET_CLOSED_AT: LocalTime = LocalTime.of(22, 30)
        private const val BACKFILL_CONCURRENCY = 8
    }

    @Scheduled(cron = SchedulerCron.BACKTEST_BACKFILL, scheduler = "slowScheduler")
    fun scheduledBackfill() {
        log.info { "BacktestBackfillScheduler cron fired" }
        schedulerLogService.markFired(SchedulerName.BacktestBackfillScheduler)
        if (holidayService.isHoliday()) {
            log.info { "BacktestBackfillScheduler skipped: today is holiday" }
            return
        }
        runBackfill()
    }

    fun runBackfill() {
        schedulerLogService.execute(SchedulerName.BacktestBackfillScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                runBlocking { doBackfill() }
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private suspend fun doBackfill() {
        val lastClosed = lastClosedTradingDay()
        val lastClosedKey = lastClosed.format(YYYYMMDD)

        val candidates = stockPickHistoryRepository.findBackfillCandidates()
        if (candidates.isEmpty()) {
            log.info { "BacktestBackfill: 미충전 픽 없음" }
            return
        }

        val refsByPickDay = candidates
            .map { it.pickDate.toLocalDate() }
            .distinct()
            .associateWith { pickDay ->
                Triple(
                    nthTradingDayAfter(pickDay, 1),
                    nthTradingDayAfter(pickDay, 5),
                    nthTradingDayAfter(pickDay, 20),
                )
            }

        val targets = candidates.filter { history ->
            val (ref1d, ref5d, ref20d) = refsByPickDay.getValue(history.pickDate.toLocalDate())
            ((history.priceOpen1d == null || history.priceClose1d == null) && ref1d <= lastClosed) ||
                (history.priceClose5d == null && ref5d <= lastClosed) ||
                (history.priceClose20d == null && ref20d <= lastClosed)
        }
        if (targets.isEmpty()) {
            log.info { "BacktestBackfill: 캐치업 대상 없음 (기준일 미도래 ${candidates.size}건)" }
            return
        }

        log.info { "BacktestBackfill 시작 — 대상 history=${targets.size} (마지막 마감 거래일=$lastClosed)" }

        val byStock = targets.groupBy { it.stkCd }

        val changed = byStock.entries.toList()
            .mapConcurrently(BACKFILL_CONCURRENCY) { (stkCd, list) -> backfillStock(stkCd, list, refsByPickDay, lastClosed, lastClosedKey) }
            .flatten()

        stockPickHistoryRepository.saveAll(changed)
        log.info { "BacktestBackfill 완료 — 채움=${changed.size} / 대상=${targets.size}" }
    }

    private suspend fun backfillStock(
        stkCd: String,
        list: List<StockPickHistory>,
        refsByPickDay: Map<LocalDate, Triple<LocalDate, LocalDate, LocalDate>>,
        lastClosed: LocalDate,
        lastClosedKey: String,
    ): List<StockPickHistory> {
        return try {
            val res = stockChartClient.chartDayList(
                req = KiwoomStockChartDayReq(
                    stk_cd = stkCd,
                    base_dt = lastClosedKey,
                    upd_stkpc_tp = "1",
                )
            )
            if (res.return_code != 0) {
                log.warn { "chartDayList 실패 stkCd=$stkCd return_code=${res.return_code}" }
                return emptyList()
            }
            val rowByDt = (res.stk_dt_pole_chart_qry ?: emptyList()).associateBy { it.dt }
            list.filter { history -> fillHistory(history, rowByDt, refsByPickDay.getValue(history.pickDate.toLocalDate()), lastClosed) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "BacktestBackfill 종목 처리 실패 stkCd=$stkCd" }
            emptyList()
        }
    }

    private fun fillHistory(
        history: StockPickHistory,
        rowByDt: Map<String?, KiwoomStockChartDay>,
        refs: Triple<LocalDate, LocalDate, LocalDate>,
        lastClosed: LocalDate,
    ): Boolean {
        val (ref1d, ref5d, ref20d) = refs
        var touched = false

        if ((history.priceOpen1d == null || history.priceClose1d == null) && ref1d <= lastClosed) {
            rowByDt[ref1d.format(YYYYMMDD)]?.let { row ->
                row.open_pric?.toLongOrNull()?.let { history.priceOpen1d = abs(it) }
                row.cur_prc?.toLongOrNull()?.let { history.priceClose1d = abs(it) }
                touched = true
            }
        }
        if (history.priceClose5d == null && ref5d <= lastClosed) {
            rowByDt[ref5d.format(YYYYMMDD)]?.cur_prc?.toLongOrNull()?.let {
                history.priceClose5d = abs(it)
                touched = true
            }
        }
        if (history.priceClose20d == null && ref20d <= lastClosed) {
            rowByDt[ref20d.format(YYYYMMDD)]?.cur_prc?.toLongOrNull()?.let {
                history.priceClose20d = abs(it)
                touched = true
            }
        }
        return touched
    }

    private fun lastClosedTradingDay(): LocalDate = holidayService.lastClosedTradingDay(closedAt = MARKET_CLOSED_AT)

    private fun nthTradingDayAfter(from: LocalDate, n: Int): LocalDate {
        var date = from
        repeat(n) { date = holidayService.nextTradingDay(date) }
        return date
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
