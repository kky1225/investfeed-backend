package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.repository.AssistantIndexAlertRepository
import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.circuitbreaker.service.CircuitBreakerService
import com.example.investfeed.global.holiday.HolidayService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 비서 채팅 "서킷브레이커 발동했어?" (get_circuit_breakers).
 * 발동했으면 이미 게시한 서킷브레이커 알림 카드를 그대로 보여 주고, 없으면 카드 없이 문장(reply)으로 답한다.
 * 국내는 코스피·코스닥이 따로 걸리고, 미국은 S&P500 기준으로 시장 전체가 한 번에 멈춘다 (나스닥 단독 CB 없음)
 */
@Service
class CircuitBreakerToolService(
    private val alertRepository: AssistantIndexAlertRepository,
    private val circuitBreakerService: CircuitBreakerService,
    private val holidayService: HolidayService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val objectMapper: ObjectMapper,
) {
    enum class CbMarket { KR, US }

    enum class CbIndex(val label: String) { KOSPI("코스피"), KOSDAQ("코스닥"), SP500("S&P500"), NASDAQ("나스닥"), DOW("다우") }

    companion object {
        private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val KR_NAME = mapOf("KOSPI" to "코스피", "KOSDAQ" to "코스닥")
        private const val US_INDEX = "SP500"
        private val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
    }

    data class AlertCard(val alertId: Long, val asOf: LocalDateTime, val body: JsonNode)

    /** reply 가 있으면 카드 없이 문장으로 답한다 (발동 없음 등) */
    data class Answer(val market: CbMarket, val tradeDate: LocalDate, val reply: String?, val alerts: List<AlertCard>)

    /** date 미지정 = 오늘(휴장일이면 직전 거래일). 지정하면 그날만 답한다 (미국은 현지 날짜) */
    fun circuitBreakers(market: CbMarket, index: CbIndex?, date: LocalDate?): Answer = when (market) {
        CbMarket.KR -> kr(index, date)
        CbMarket.US -> us(index, date)
    }

    private fun kr(index: CbIndex?, asked: LocalDate?): Answer {
        val today = LocalDate.now()
        val codes = index?.takeIf { it == CbIndex.KOSPI || it == CbIndex.KOSDAQ }?.let { listOf(it.name) } ?: listOf("KOSPI", "KOSDAQ")
        val names = codes.joinToString("·") { KR_NAME[it] ?: it }

        // 날짜를 직접 물었으면 그날만 본다. 휴장일·미래면 직전 거래일로 넘기지 않고 그대로 알린다
        if (asked != null) {
            val day = TemplateFormat.monthDay(asked)
            if (asked.isAfter(today)) return Answer(CbMarket.KR, asked, "${day}은 아직 장이 열리지 않았습니다.", emptyList())
            if (holidayService.isHoliday(asked)) return Answer(CbMarket.KR, asked, "${day}은 휴장일이라 서킷브레이커가 없습니다.", emptyList())
        }
        val date = asked ?: holidayService.lastTradingDay(today)
        // 오늘을 물었는데 휴장일이면 휴장임을 먼저 알리고 직전 거래일 결과를 답한다
        val prefix = if (asked == null && date != today) "오늘 ${TemplateFormat.monthDay(today)}은 휴장일입니다. 직전 거래일 " else ""

        val alerts = cards(date, codes)
        if (alerts.isNotEmpty()) return Answer(CbMarket.KR, date, null, alerts)

        val events = circuitBreakerService.findByDate(date).filter { it.indexCode in codes }.sortedBy { it.startedAt }
        val reply = prefix + if (events.isEmpty()) {
            "${TemplateFormat.monthDay(date)} $names 서킷브레이커는 발동하지 않았습니다."
        } else {
            "${TemplateFormat.monthDay(date)} " + events.joinToString(", ") { e ->
                "${KR_NAME[e.indexCode] ?: e.indexCode} 서킷브레이커 ${e.stage}단계 ${e.startedAt.format(HHMM)}~${e.endedAt?.format(HHMM) ?: "장 종료"}"
            }
        }
        return Answer(CbMarket.KR, date, reply, emptyList())
    }

    private fun us(index: CbIndex?, asked: LocalDate?): Answer {
        // 미국 장이 열려 있으면 진행 중인 거래일, 아니면 직전에 끝난 거래일
        val nowEt = LocalDateTime.now().atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val todayEt = nowEt.toLocalDate()
        val current = todayEt.takeIf { usMarketCalendarService.isUsTradingDay(it) && !nowEt.toLocalTime().isBefore(US_OPEN_ET) }
            ?: usMarketCalendarService.lastClosedUsTradingDay()
        // 나스닥·다우로 물어도 지수별 CB 는 없어 S&P500 기준 시장 전체 결과로 답한다
        val note = index?.takeIf { it == CbIndex.NASDAQ || it == CbIndex.DOW }
            ?.let { "${it.label} 단독 서킷브레이커는 없고, S&P500 기준으로 미국 시장 전체가 멈춥니다. " }.orEmpty()

        if (asked != null) {
            val day = "현지 ${TemplateFormat.monthDay(asked)}"
            if (asked.isAfter(current)) return Answer(CbMarket.US, asked, "${note}미국 증시는 $day 장이 아직 열리지 않았습니다.", emptyList())
            if (!usMarketCalendarService.isUsTradingDay(asked)) return Answer(CbMarket.US, asked, "${note}미국 증시는 $day 휴장일이라 서킷브레이커가 없습니다.", emptyList())
        }
        val date = asked ?: current
        val alerts = cards(date, listOf(US_INDEX))
        if (alerts.isNotEmpty()) return Answer(CbMarket.US, date, null, alerts)

        // 오늘을 물었는데 미국 휴장일(현지 날짜 기준)이면 휴장임을 먼저 알린다. 거래일 개장 전은 직전 장 결과만 답한다
        val holiday = if (asked == null && !usMarketCalendarService.isUsTradingDay(todayEt)) "미국 증시는 현지 ${TemplateFormat.monthDay(todayEt)} 휴장입니다. 직전 거래일 " else ""
        val reply = "$note$holiday${TemplateFormat.monthDay(date)} 미국 증시(S&P500 기준) 서킷브레이커는 발동하지 않았습니다."
        return Answer(CbMarket.US, date, reply, emptyList())
    }

    private fun cards(date: LocalDate, indexCodes: List<String>): List<AlertCard> =
        alertRepository.findByAlertDateAndTriggerTypeOrderByFiredAt(date, "CB")
            .filter { it.indexCode in indexCodes }
            .map { AlertCard(it.id, it.firedAt, objectMapper.readTree(it.body)) }
}
