package com.example.investfeed.global.holiday

import com.example.investfeed.common.util.failure
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicReference

/**
 * 국내 장 시각을 따라 도는 작업의 예약. cron 대신 그날 장 시각으로 "다음 실행 시각"을 계산해 그 시각에만 깨운다.
 * - registerDaily: 하루 한 번 (모의투자 1차·2차, 국내 마감 브리핑) — 평소 시각(normalCron)에 개장·마감이 밀린 만큼 더한 시각.
 *   예: 모의투자 1차 평소 08:50 → 수능일 09:50
 * - registerDuringSession: 장중 반복 (지수 알림·서킷브레이커·매크로 수집) — 거래일의 개장부터 정한 끝까지 step 간격.
 *   예: 지수 알림 평소 09:00:01~15:30:01 매분 → 수능일 10:00:01~16:30:01. 휴장일·장 밖에는 깨어나지 않는다
 *
 * 다음 실행 시각은 직전 실행이 끝날 때 정해지므로, 장 시각 변경일을 등록·수정·삭제하면 reschedule() 로 다시 계산한다
 */
@Component
class KrSessionTaskScheduler(
    @Qualifier("slowScheduler") private val slowScheduler: TaskScheduler,
    @Qualifier("fastScheduler") private val fastScheduler: TaskScheduler,
    private val krMarketSessionService: KrMarketSessionService,
    private val holidayService: HolidayService,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val MAX_LOOKAHEAD_DAYS = 400
        private val RETRY_AFTER_FAILURE: Duration = Duration.ofMinutes(1)
    }

    /** 장중 반복 작업이 언제까지 도는지 */
    enum class WindowEnd {
        CLOSE_EXCLUSIVE,          // 마감 전까지 (정규장 중 — 마감 시각은 포함 안 함)
        CLOSE_INCLUSIVE,          // 마감 시각 회차까지
        CLOSE_SETTLED_INCLUSIVE,  // 마감 +30분 회차까지 (마감 확정값 수집)
    }

    private class Task(
        val name: String,
        val scheduler: TaskScheduler,
        val oncePerDay: Boolean,
        val nextAfter: (LocalDateTime) -> LocalDateTime?,
        val action: () -> Unit,
    ) {
        @Volatile var future: ScheduledFuture<*>? = null
        val lastRunDate = AtomicReference<LocalDate?>(null)
        /**
         * true 면 다음에 깨어났을 때 작업을 하지 않는다.
         * 다음 실행 시각 계산(DB 조회)이 실패하면 1분 뒤에 임시로 깨워 다시 계산하는데,
         * 그 1분 뒤는 원래 작업할 시각이 아니기 때문. 다시 계산에 성공하면 false 로 돌아간다
         */
        @Volatile var skipNextRun = false
    }

    private val tasks = CopyOnWriteArrayList<Task>()
    @Volatile private var started = false

    /** 하루 한 번 도는 작업 (slowScheduler). 앱 기동이 끝난 뒤부터 예약된다 */
    fun registerDaily(name: String, normalCron: String, anchor: KrSessionAnchor, action: () -> Unit) =
        add(Task(name, slowScheduler, oncePerDay = true, nextAfter = { from -> nextDaily(name, normalCron, anchor, from) }, action = action))

    /**
     * 장중 반복 작업 (fastScheduler). 거래일마다 개장+offset 부터 step 간격으로, end 까지.
     * offset 은 cron 의 "1초에 시작" 규칙과 맞추려는 값 (매분 작업 1초, 매초 작업 0)
     */
    fun registerDuringSession(name: String, step: Duration, offset: Duration, end: WindowEnd, action: () -> Unit) =
        add(Task(name, fastScheduler, oncePerDay = false, nextAfter = { from -> nextInSession(name, step, offset, end, from) }, action = action))

    private fun add(task: Task) {
        tasks += task
        if (started) schedule(task)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        started = true
        tasks.forEach(::schedule)
    }

    /** 장 시각 변경일이 바뀌었을 때 — 이미 정해 둔 다음 실행 시각을 다시 계산 */
    fun reschedule() {
        if (!started) return
        tasks.forEach(::schedule)
    }

    /**
     * 매일 휴장일 동기화 뒤 — 장 시각 변경일을 DB 에서 다시 읽고(직접 넣은 행도 반영) 예약을 다시 계산.
     * 휴장일이 새로 생기거나 없어졌을 때 휴장일에 돌거나 거래일을 건너뛰지 않게 하는 것도 이것으로 처리
     */
    @EventListener(MarketCalendarRefreshedEvent::class)
    fun onMarketCalendarRefreshed() {
        krMarketSessionService.evict()
        reschedule()
    }

    /** 앱 종료 시 몇 시간 뒤로 잡힌 예약 때문에 스케줄러 종료가 기다리지 않게 취소 */
    @PreDestroy
    fun stop() {
        started = false
        tasks.forEach { it.future?.cancel(false) }
    }

    @Synchronized
    private fun schedule(task: Task) {
        task.future?.cancel(false)   // 실행 중이면 끝까지 돌게 둔다
        task.future = task.scheduler.schedule({ run(task) }, trigger(task))
        log.debug { "[${task.name}] 예약" }
    }

    /**
     * 하루 한 번 작업은 같은 날 두 번 돌지 않게 한다 — 그날 실행이 끝난 뒤 관리자가 그날 장 시각을 늦추면 다시 계산한 시각에 또 잡히기 때문.
     * 기억은 메모리라 재시작하면 초기화된다
     */
    private fun run(task: Task) {
        if (task.skipNextRun) return   // 다시 계산하려고 임시로 깨운 것 — 작업은 하지 않음 (끝나면 Spring 이 트리거로 다시 계산)
        if (task.oncePerDay) {
            val today = LocalDate.now()
            if (task.lastRunDate.getAndSet(today) == today) {
                log.debug { "[${task.name}] 오늘 이미 실행 — 건너뜀" }
                return
            }
        }
        task.action()
    }

    /**
     * 직전 예정 시각(없으면 지금) 이후의 첫 실행 시각.
     * 직전 "예정" 시각 기준이라, 스케줄러가 예정보다 조금 일찍 시작해 끝나도 같은 시각을 다시 잡지 않는다.
     * 계산에 실패하면(장 시각·휴장일 DB 조회 오류) 예외를 그대로 두면 예약이 조용히 끊기므로, 기록하고 1분 뒤 다시 계산한다
     */
    private fun trigger(task: Task) = Trigger { ctx ->
        val from = listOfNotNull(ctx.lastScheduledExecution(), ctx.lastCompletion()).maxOrNull()
            ?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
            ?: LocalDateTime.now()
        val next = try {
            task.nextAfter(from).also { task.skipNextRun = false }   // 계산 성공 → 다음 깨어남은 정상 실행
        } catch (e: Exception) {
            log.failure(e) { "[${task.name}] 다음 실행 시각 계산 실패 — ${RETRY_AFTER_FAILURE.toMinutes()}분 뒤 다시 계산" }
            task.skipNextRun = true   // 1분 뒤는 원래 작업 시각이 아니므로 그때는 작업하지 않게
            LocalDateTime.now().plus(RETRY_AFTER_FAILURE)
        }
        next?.atZone(ZoneId.systemDefault())?.toInstant()
    }

    /**
     * 하루 한 번 작업의 다음 실행 시각 — from 보다 뒤인 첫 "그날 실행 시각".
     * 오늘부터 하루씩 넘기며 KrMarketSessionService.dueAt(평소 cron 시각 + 그날 개장·마감이 밀린 만큼)을 구해, from 보다 뒤면 그 시각.
     * 평소 cron 이 안 도는 요일(예: MON-FRI 의 주말)은 dueAt 이 null 이라 건너뛴다. 휴장일 여부는 작업 본문이 판정
     *
     * 예) 모의투자 1차, 평소 cron "1 50 8 * * *"
     *  - from = 수 08:50:01 (방금 실행) → 수요일 08:50:01 은 from 과 같아 통과 → 목요일(수능일) 09:50:01
     *  - from = 목 09:50:01 → 금요일 08:50:01
     */
    private fun nextDaily(name: String, normalCron: String, anchor: KrSessionAnchor, from: LocalDateTime): LocalDateTime? {
        var date = from.toLocalDate()
        repeat(MAX_LOOKAHEAD_DAYS) {
            krMarketSessionService.dueAt(date, normalCron, anchor)
                ?.takeIf { it.isAfter(from) }
                ?.let { return it }
            date = date.plusDays(1)
        }
        log.warn { "[$name] ${MAX_LOOKAHEAD_DAYS}일 안에 실행 시각이 없음 (cron=$normalCron) — 예약 중단" }
        return null
    }

    /**
     * 장중 반복 작업의 다음 실행 시각 — from 보다 뒤인 첫 회차.
     * 회차는 거래일마다 "첫 회차(개장 + offset)부터 step 간격"으로 찍힌 눈금이고, 마지막 회차는 end 로 정한다.
     * 1. 휴장일이면 그날은 건너뛴다
     * 2. from 이 첫 회차보다 이르면 첫 회차, 아니면 from 바로 다음 눈금
     * 3. 그 눈금이 마지막 회차 이내면 확정, 넘으면 다음 날로 넘어가 다시 1부터 (다음 날은 from 이 첫 회차보다 이르므로 첫 회차)
     *
     * 예) 지수 알림: step 1분, offset 1초, CLOSE_INCLUSIVE → 평소 09:00:01 ~ 15:30:01
     *  - from = 08:30 → 09:00:01
     *  - from = 10:15:01 → 10:16:01
     *  - from = 금 15:30:01 → 15:31:01 은 마지막 회차를 넘음 → 토·일 휴장 건너뜀 → 월 09:00:01 (월요일이 수능일이면 10:00:01)
     */
    private fun nextInSession(name: String, step: Duration, offset: Duration, end: WindowEnd, from: LocalDateTime): LocalDateTime? {
        var date = from.toLocalDate()
        repeat(MAX_LOOKAHEAD_DAYS) {
            if (!holidayService.isHoliday(date)) {
                val s = krMarketSessionService.of(date)
                val first = date.atTime(s.open).plus(offset)
                val last = when (end) {
                    WindowEnd.CLOSE_EXCLUSIVE -> date.atTime(s.close).plus(offset).minus(step)
                    WindowEnd.CLOSE_INCLUSIVE -> date.atTime(s.close).plus(offset)
                    WindowEnd.CLOSE_SETTLED_INCLUSIVE -> date.atTime(s.closeSettled).plus(offset)
                }
                val candidate = if (from.isBefore(first)) first
                else first.plus(step.multipliedBy(Duration.between(first, from).toNanos() / step.toNanos() + 1))
                if (!candidate.isAfter(last)) return candidate
            }
            date = date.plusDays(1)
        }
        log.warn { "[$name] ${MAX_LOOKAHEAD_DAYS}일 안에 거래일이 없음 — 예약 중단" }
        return null
    }
}
