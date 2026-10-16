package com.example.investfeed.domain.circuitbreaker.service

import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.circuitbreaker.dto.CircuitBreakerFiredEvent
import com.example.investfeed.domain.circuitbreaker.dto.res.CircuitBreakerRes
import com.example.investfeed.domain.marketevent.entity.MarketEvent
import com.example.investfeed.domain.marketevent.entity.MarketEventDirection
import com.example.investfeed.domain.marketevent.entity.MarketEventType
import com.example.investfeed.domain.marketevent.repository.MarketEventRepository
import com.example.investfeed.kiwoom.socket.KiwoomSocketManager
import com.example.investfeed.kiwoom.socket.PersistentStreamHandler
import com.fasterxml.jackson.databind.JsonNode
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * 코스피·코스닥 서킷브레이커 판정.
 *
 * 키움은 시장 단위 CB 발동 여부를 내려주지 않아(REST·실시간 모두 확인, 2026-10-15) 지수 실시간 체결(0J)로 직접 판정한다.
 * 거래소 규칙: 전일 대비 -8%/-15%/-20% 이하가 1분간 "연속" 유지되면 발동. 2·3단계는 직전 단계 발동 지수보다 1% 추가 하락 필요.
 * 단계별 하루 1회, 1·2단계는 14:50 이후 발동하지 않음. 1·2단계는 20분 정지 + 10분 단일가 후 재개, 3단계는 당일 장 종료.
 * 발동 이력은 market_event(event_type=CIRCUIT_BREAKER) 에 영구 저장하고, 당일분은 메모리에 두고 판정·조회에 쓴다.
 */
@Service
class CircuitBreakerService(
    private val repository: MarketEventRepository,
    private val socketManager: KiwoomSocketManager,
    private val eventPublisher: ApplicationEventPublisher,
) : PersistentStreamHandler {
    private val log = KotlinLogging.logger {}

    companion object {
        const val REALTIME_TYPE = "0J"
        val INDS_CD = mapOf("001" to "KOSPI", "101" to "KOSDAQ")

        private val LEVELS = listOf(-8.0, -15.0, -20.0)
        private const val EXTRA_DROP_RATIO = 0.99                 // 2·3단계: 직전 단계 발동 지수 대비 1% 추가 하락
        private val SUSTAIN: Duration = Duration.ofSeconds(60)
        private val HALT: Duration = Duration.ofMinutes(30)       // 20분 매매 정지 + 10분 단일가
        private val LATE_CUTOFF: LocalTime = LocalTime.of(14, 50) // 1·2단계는 이후 발동 안 함
        private val TRADE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")
    }

    private class Tracker {
        var belowSince: LocalDateTime? = null // 다음 단계 조건에 처음 들어온 체결시간. 조건을 벗어나면 초기화
        var lastTickAt: LocalDateTime? = null
        var price = 0.0
        var prevClose = 0.0
        var rate = 0.0
        var high = 0.0
        var low = 0.0
        var ticks = 0
        var maxGapSec = 0L
    }

    data class TickStats(val indexCode: String, val ticks: Int, val maxGapSec: Long)

    private val lock = Any()
    private var date: LocalDate? = null
    private val trackers = mutableMapOf<String, Tracker>()
    private val events = mutableListOf<MarketEvent>()

    // 소켓 상시 구독(0J 001·101) 수신을 받도록 소켓 매니저에 등록
    @PostConstruct
    fun registerHandler() = socketManager.addHandler(this)

    override fun onPersistentTick(type: String, item: String, values: JsonNode?) {
        if (type != REALTIME_TYPE) return
        val code = INDS_CD[item] ?: return
        val v = values ?: return
        val price = TemplateFormat.parse(v.get("10")?.asText())?.let { abs(it) }?.takeIf { it > 0 } ?: return
        val change = TemplateFormat.parse(v.get("11")?.asText()) ?: return
        val rate = TemplateFormat.parse(v.get("12")?.asText()) ?: return
        val now = LocalDateTime.now()
        val tradeAt = runCatching { now.toLocalDate().atTime(LocalTime.parse(v.get("20")?.asText(), TRADE_TIME)) }.getOrNull() ?: return

        synchronized(lock) {
            rollDate(now.toLocalDate())
            val t = trackers.getOrPut(code) { Tracker() }
            t.lastTickAt?.let { t.maxGapSec = maxOf(t.maxGapSec, Duration.between(it, now).seconds) }
            t.ticks++
            t.lastTickAt = now
            t.price = price
            t.prevClose = price - change
            t.rate = rate
            t.high = TemplateFormat.parse(v.get("17")?.asText())?.let { abs(it) } ?: maxOf(t.high, price)
            t.low = TemplateFormat.parse(v.get("18")?.asText())?.let { abs(it) } ?: price

            val stage = nextStage(code)
            t.belowSince = when {
                stage == null || !meets(code, stage, rate, price) -> null
                else -> t.belowSince ?: tradeAt
            }
        }
    }

    /** 1초마다 호출. 조건 진입 후 1분이 지나면 발동 확정 (발동 직후엔 체결이 끊기므로 체결 수신이 아니라 시계로 판정) */
    fun check(now: LocalDateTime) {
        if (!socketManager.isConnected()) return
        val fired = synchronized(lock) {
            rollDate(now.toLocalDate())
            trackers.mapNotNull { (code, t) -> confirm(code, t, now) }
        }
        fired.forEach { eventPublisher.publishEvent(it) }
    }

    private fun confirm(code: String, t: Tracker, now: LocalDateTime): CircuitBreakerFiredEvent? {
        val since = t.belowSince ?: return null
        if (Duration.between(since, now) < SUSTAIN) return null
        val stage = nextStage(code) ?: return null
        val firedAt = since.plus(SUSTAIN)
        t.belowSince = null
        if (stage < 3 && !firedAt.toLocalTime().isBefore(LATE_CUTOFF)) return null

        val event = runCatching {
            repository.save(
                MarketEvent(
                    tradeDate = firedAt.toLocalDate(), indexCode = code, eventType = MarketEventType.CIRCUIT_BREAKER,
                    direction = MarketEventDirection.DOWN, stage = stage, triggerRate = t.rate, triggerValue = t.price,
                    startedAt = firedAt, endedAt = if (stage < 3) firedAt.plus(HALT) else null,
                )
            )
        }.onFailure { log.error(it) { "서킷브레이커 발동 저장 실패: $code ${stage}단계 $firedAt" } }.getOrNull() ?: return null
        events += event
        return CircuitBreakerFiredEvent(event, t.prevClose, t.high, t.low)
    }

    private fun nextStage(code: String): Int? =
        ((events.filter { it.indexCode == code }.maxOfOrNull { it.stage } ?: 0) + 1).takeIf { it <= LEVELS.size }

    private fun meets(code: String, stage: Int, rate: Double, price: Double): Boolean {
        if (rate > LEVELS[stage - 1]) return false
        if (stage == 1) return true
        val prev = events.lastOrNull { it.indexCode == code && it.stage == stage - 1 } ?: return false
        return price <= prev.triggerValue * EXTRA_DROP_RATIO
    }

    fun findToday(now: LocalDateTime = LocalDateTime.now()): List<CircuitBreakerRes> = synchronized(lock) {
        rollDate(now.toLocalDate())
        events.map {
            CircuitBreakerRes(it.indexCode, it.stage, it.triggerRate, it.startedAt, it.endedAt, isHalted(it, now))
        }
    }

    /** 특정 거래일의 발동 기록. 브리핑·비서 채팅처럼 날짜가 정해진 조회용 */
    fun findByDate(date: LocalDate): List<MarketEvent> = synchronized(lock) {
        if (date == this.date) events.toList() else repository.findByTradeDateAndEventTypeOrderByStartedAt(date, MarketEventType.CIRCUIT_BREAKER)
    }

    /** 지수별 0J 수신 건수·최대 수신 간격. 조회 후 초기화 (스케줄러 로그용) */
    fun drainTickStats(): List<TickStats> = synchronized(lock) {
        trackers.map { (code, t) -> TickStats(code, t.ticks, t.maxGapSec).also { t.ticks = 0; t.maxGapSec = 0 } }
    }

    private fun isHalted(e: MarketEvent, now: LocalDateTime) = e.endedAt?.let { now < it } ?: true

    // 날짜가 바뀌면 메모리 상태를 비우고 그날 기록을 DB 에서 복원 (재시작 시 같은 단계 재발동 방지)
    private fun rollDate(today: LocalDate) {
        if (date == today) return
        date = today
        trackers.clear()
        events.clear()
        events += repository.findByTradeDateAndEventTypeOrderByStartedAt(today, MarketEventType.CIRCUIT_BREAKER)
    }
}
