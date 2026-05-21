package com.example.investfeed.domain.recommend.service

import com.example.investfeed.common.util.DateUtil
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
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
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * 백테스트 인프라 — N영업일 후 시가/종가 백필 스케줄러.
 *
 * 매일 22:30 (RecommendScheduler 22:00 직후) 실행:
 * - 어제(T-1영업일) 추천 history → priceOpen1d, priceClose1d (오늘 시가/종가)
 * - 5영업일 전 추천 history → priceClose5d (오늘 종가)
 * - 20영업일 전 추천 history → priceClose20d (오늘 종가)
 *
 * 백테스트 모델 가정: T일 22:00 추천 → T+1일 시가 매수 → T+N일 종가 평가.
 * 따라서 N영업일 후의 종가는 항상 **오늘** 종가이므로, 오늘 22:30 시점 chartDayList 응답 첫 row 활용.
 *
 * 외부 API 추가 호출 없이 기존 [StockChartClient.chartDayList] 재활용.
 */
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
        private const val API_PACING_MS = 200L  // RecommendService 와 동일 페이싱
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    @Scheduled(cron = "0 30 22 * * *", scheduler = "slowScheduler")
    fun scheduledBackfill() {
        log.info { "BacktestBackfillScheduler cron fired" }
        if (holidayService.isHoliday()) {
            log.info { "BacktestBackfillScheduler skipped: today is holiday" }
            return
        }
        runBackfill()
    }

    @Transactional
    fun runBackfill() {
        schedulerLogService.execute(SchedulerName.BacktestBackfillScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
                doBackfill()
            } finally {
                SecurityContextHolder.clearContext()
            }
        }
    }

    private fun doBackfill() {
        val today = LocalDate.now()
        // 오늘 기준 N 영업일 전 일자 (T-1, T-5, T-20 = 추천일자 pickDate 의 LocalDate)
        val date1d = nthLastTradingDayBefore(today, 1)
        val date5d = nthLastTradingDayBefore(today, 5)
        val date20d = nthLastTradingDayBefore(today, 20)

        // 대상 history 모두 조회 (같은 날짜 중복 제거)
        val datesByOffset = listOf(
            date1d to BacktestOffset.D1,
            date5d to BacktestOffset.D5,
            date20d to BacktestOffset.D20,
        )

        val pickDateToOffsets: Map<LocalDate, Set<BacktestOffset>> = datesByOffset
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.toSet() }

        val histories = pickDateToOffsets.keys.flatMap { d ->
            stockPickHistoryRepository.findByPickDateBetween(
                d.atStartOfDay(),
                d.atTime(23, 59, 59),
            )
        }

        if (histories.isEmpty()) {
            log.info { "BacktestBackfill: 대상 history 없음 (date1d=$date1d, date5d=$date5d, date20d=$date20d)" }
            return
        }

        log.info {
            "BacktestBackfill 시작 — 대상 history=${histories.size} " +
                "(date1d=$date1d, date5d=$date5d, date20d=$date20d)"
        }

        // 종목별로 묶어서 chartDayList 1회 호출 (같은 종목이 여러 N 에 걸쳐도 1회)
        val byStock = histories.groupBy { it.stkCd }
        var filled = 0

        for ((stkCd, list) in byStock) {
            try {
                Thread.sleep(API_PACING_MS)
                val res = stockChartClient.chartDayList(
                    req = KiwoomStockChartDayReq(
                        stk_cd = stkCd,
                        base_dt = DateUtil.today("yyyyMMdd"),
                        upd_stkpc_tp = "1",
                    )
                )
                if (res.return_code != 0) {
                    log.warn { "chartDayList 실패 stkCd=$stkCd return_code=${res.return_code}" }
                    continue
                }
                val rows = res.stk_dt_pole_chart_qry ?: continue

                // 응답 첫 row = today (가장 최근 일봉). 시가/종가 추출.
                val todayKey = today.format(YYYYMMDD)
                val todayRow = rows.firstOrNull { it.dt == todayKey } ?: rows.firstOrNull()
                val todayOpen = todayRow?.open_pric?.toLongOrNull()?.let { abs(it) }
                val todayClose = todayRow?.cur_prc?.toLongOrNull()?.let { abs(it) }

                if (todayOpen == null && todayClose == null) {
                    log.warn { "chartDayList 응답에 오늘 가격 없음 stkCd=$stkCd today=$todayKey" }
                    continue
                }

                for (history in list) {
                    val pickLocalDate = history.pickDate.toLocalDate()
                    val offsets = pickDateToOffsets[pickLocalDate] ?: continue
                    if (BacktestOffset.D1 in offsets) {
                        history.priceOpen1d = todayOpen
                        history.priceClose1d = todayClose
                    }
                    if (BacktestOffset.D5 in offsets) {
                        history.priceClose5d = todayClose
                    }
                    if (BacktestOffset.D20 in offsets) {
                        history.priceClose20d = todayClose
                    }
                    filled++
                }
            } catch (e: Exception) {
                log.warn(e) { "BacktestBackfill 종목 처리 실패 stkCd=$stkCd" }
            }
        }

        stockPickHistoryRepository.saveAll(histories)
        log.info { "BacktestBackfill 완료 — 채움=$filled / 대상=${histories.size}" }
    }

    /**
     * 오늘 기준 N 영업일 전 일자. n=1 이면 직전 거래일 (어제가 영업일이면 어제, 아니면 그 이전).
     */
    private fun nthLastTradingDayBefore(from: LocalDate, n: Int): LocalDate {
        var date = from
        repeat(n) {
            date = holidayService.lastTradingDay(date.minusDays(1))
        }
        return date
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    private enum class BacktestOffset { D1, D5, D20 }
}
