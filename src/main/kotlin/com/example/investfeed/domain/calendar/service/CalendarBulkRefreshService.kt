package com.example.investfeed.domain.calendar.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

data class BulkRefreshStatus(
    val running: Boolean,
    val yearFrom: Int?,
    val yearTo: Int?,
    val totalMonths: Int,
    val processedMonths: Int,
    val failedMonths: Int,
    val currentMonth: String?,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val errorMessage: String?,
)

/**
 * 캘린더 이벤트 일괄 재생성 서비스.
 *
 * - 지정 연도 범위의 모든 월에 대해 FRED/ECOS/공휴일 API 재호출
 * - 기존 API 이벤트 DB 삭제 + 새로 저장 (MANUAL 이벤트 보존)
 * - Redis 캐시 무효화
 * - 진행 상황 in-memory 추적 (서버 재기동 시 초기화)
 * - 한 번에 하나의 bulk refresh 만 허용 (중복 실행 방지)
 */
@Service
class CalendarBulkRefreshService(
    private val economicCalendarService: EconomicCalendarService,
    @Qualifier("slowScheduler") private val taskScheduler: TaskScheduler,
) {
    private val log = KotlinLogging.logger {}

    private val status = AtomicReference(
        BulkRefreshStatus(
            running = false, yearFrom = null, yearTo = null,
            totalMonths = 0, processedMonths = 0, failedMonths = 0,
            currentMonth = null, startedAt = null, finishedAt = null, errorMessage = null,
        )
    )

    fun getStatus(): BulkRefreshStatus = status.get()

    fun start(yearFrom: Int, yearTo: Int) {
        if (yearFrom > yearTo) throw IllegalArgumentException("yearFrom 은 yearTo 이하여야 합니다.")
        if (yearFrom < 2000 || yearTo > LocalDateTime.now().year + 1) {
            throw IllegalArgumentException("연도 범위가 비정상입니다.")
        }

        val current = status.get()
        if (current.running) {
            throw IllegalStateException("이미 일괄 재생성이 실행 중입니다.")
        }

        val now = LocalDateTime.now()
        // 미래 월(현재+2 이후) 은 skip 되므로 실제 처리 예정 월 수만 계산
        val maxYm = YearMonth.now().plusMonths(2)
        val fromYm = YearMonth.of(yearFrom, 1)
        val toYm = minOf(YearMonth.of(yearTo, 12), maxYm)
        val totalMonths = if (toYm.isBefore(fromYm)) 0
            else (ChronoUnit.MONTHS.between(fromYm, toYm).toInt() + 1)

        status.set(BulkRefreshStatus(
            running = true,
            yearFrom = yearFrom, yearTo = yearTo,
            totalMonths = totalMonths, processedMonths = 0, failedMonths = 0,
            currentMonth = null, startedAt = now, finishedAt = null, errorMessage = null,
        ))

        taskScheduler.schedule({ execute(yearFrom, yearTo) }, Instant.now())
        log.info { "[bulk-refresh] 시작: $yearFrom-$yearTo (총 $totalMonths 개월)" }
    }

    private fun execute(yearFrom: Int, yearTo: Int) {
        try {
            var processed = 0
            var failed = 0

            for (year in yearFrom..yearTo) {
                for (month in 1..12) {
                    // 미래 월은 건너뜀
                    val ym = YearMonth.of(year, month)
                    if (ym.isAfter(YearMonth.now().plusMonths(2))) continue

                    status.updateAndGet {
                        it.copy(currentMonth = "$year-${String.format("%02d", month)}")
                    }

                    try {
                        economicCalendarService.refreshEvents(year, month)
                        processed++
                    } catch (e: Exception) {
                        log.warn { "[bulk-refresh] $year-$month 재생성 실패: ${e.message}" }
                        failed++
                    }

                    status.updateAndGet {
                        it.copy(processedMonths = processed, failedMonths = failed)
                    }
                }
            }

            status.updateAndGet {
                it.copy(
                    running = false,
                    currentMonth = null,
                    finishedAt = LocalDateTime.now(),
                )
            }
            log.info { "[bulk-refresh] 완료: 성공 $processed / 실패 $failed" }
        } catch (e: Exception) {
            log.error { "[bulk-refresh] 전체 실패: ${e.message}" }
            status.updateAndGet {
                it.copy(
                    running = false,
                    finishedAt = LocalDateTime.now(),
                    errorMessage = e.message,
                )
            }
        }
    }
}
