package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.ReleaseFact
import com.example.investfeed.domain.assistant.entity.AssistantReleaseAlert
import com.example.investfeed.domain.assistant.repository.AssistantReleaseAlertRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.calendar.dto.res.CalendarEvent
import com.example.investfeed.domain.calendar.service.EconomicCalendarService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private fun CalendarEvent.isUsFred() = country == "US" && source == "FRED" && type == "INDICATOR"

/** 발표 알림 대상 지표. 캘린더 이벤트에 고정 id·시리즈 코드가 없어 국가·소스·이름으로 식별한다 */
enum class ReleaseTarget(
    val label: String,
    val country: String,
    val historyCode: String,       // EconomicCalendarService.getIndicatorHistory 코드 (US=seriesId, KR=tableCode)
    val prevLabel: String,
    val matcher: (CalendarEvent) -> Boolean,
) {
    US_FOMC("FOMC 기준금리", "US", "DFEDTARU", "직전", { it.type == "US_RATE_DECISION" }),
    KR_BASE_RATE("한국 기준금리", "KR", "722Y001", "직전", { it.type == "RATE_DECISION" }),
    KR_GDP("한국 GDP 성장률", "KR", "200Y102", "전기", { it.type == "GDP_RELEASE" }),
    US_CPI("미국 CPI", "US", "CPIAUCSL", "전월", { it.isUsFred() && it.name.endsWith("소비자물가지수(전년동월비)") }),
    US_PCE("미국 PCE 물가", "US", "PCEPI", "전월", { it.isUsFred() && it.name.endsWith("PCE 물가지수(전년동월비)") }),
    US_PAYROLL("미국 비농업고용", "US", "PAYEMS", "전월", { it.isUsFred() && it.name.endsWith("비농업고용") }),
    US_UNRATE("미국 실업률", "US", "UNRATE", "전월", { it.isUsFred() && it.name.endsWith("실업률") }),
    US_GDP("미국 GDP 성장률", "US", "A191RL1Q225SBEA", "전기", { it.isUsFred() && it.name.contains(" GDP ") }),
    KR_CPI("한국 CPI", "KR", "901Y009", "전월", { it.country == "KR" && it.source == "ECOS" && it.name == "소비자물가지수(전년동월비)" });

    fun isFresh(eventDate: LocalDate, today: LocalDate): Boolean = when (this) {
        KR_CPI -> YearMonth.from(eventDate).let { it == YearMonth.from(today) || it == YearMonth.from(today).minusMonths(1) }
        else -> !eventDate.isBefore(today.minusDays(7))
    }

    companion object {
        fun match(e: CalendarEvent): ReleaseTarget? = entries.firstOrNull { it.matcher(e) }
    }
}

@Service
class ReleaseAlertService(
    private val economicCalendarService: EconomicCalendarService,
    private val releaseRepository: AssistantReleaseAlertRepository,
    private val assistantSettingService: AssistantSettingService,
    private val memberRepository: MemberRepository,
    private val timelineService: TimelineService,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    data class RunResult(val scanned: Int, val saved: Int, val suppressed: Int, val posted: Int, val seed: Boolean)

    fun run(now: LocalDateTime = LocalDateTime.now()): RunResult {
        val today = now.toLocalDate()
        val months = listOf(YearMonth.from(today), YearMonth.from(today).minusMonths(1))
        val events = months.flatMap { ym ->
            runCatching { economicCalendarService.listEvents(ym.year, ym.monthValue).events }
                .onFailure { log.error(it) { "발표 알림 캘린더 조회 실패: $ym" } }.getOrDefault(emptyList())
        }
        val seed = releaseRepository.count() == 0L
        var saved = 0; var suppressedCount = 0; var posted = 0
        events.forEach { e ->
            val target = ReleaseTarget.match(e) ?: return@forEach
            val value = e.value?.takeIf { it.isNotBlank() } ?: return@forEach
            if (e.isFuture) return@forEach
            val eventDate = runCatching { LocalDate.parse(e.date) }.getOrNull() ?: return@forEach
            if (releaseRepository.existsByCountryAndEventDateAndEventName(e.country, eventDate, e.name)) return@forEach

            val suppressed = seed || !target.isFresh(eventDate, today)
            val prev = if (suppressed) null else prevValue(target, eventDate)
            val fact = ReleaseFact(target.name, e.country, eventDate, e.name, value, prev, now)
            val body = AlertTemplateRenderer.renderRelease(fact, target)
            val entity = try {
                releaseRepository.save(
                    AssistantReleaseAlert(
                        targetKey = target.name, country = e.country, eventDate = eventDate, eventName = e.name.take(200),
                        value = value.take(100), prevValue = prev?.take(100), suppressed = suppressed, detectedAt = now,
                        headlineText = body.headline.text.take(300), body = objectMapper.writeValueAsString(body),
                    )
                )
            } catch (ex: DataIntegrityViolationException) {
                return@forEach
            }
            saved++
            if (suppressed) { suppressedCount++; return@forEach }
            val finalBody = body.copy(refs = mapOf("alertId" to entity.id))
            memberRepository.findAll()
                .filter { assistantSettingService.findOrDefault(it.id).releaseAlertEnabled }
                .forEach { member ->
                    runCatching { timelineService.postMessage(member.id, finalBody, refAlertId = entity.id); posted++ }
                        .onFailure { log.error(it) { "발표 알림 회원 게시 실패: member=${member.loginId} alertId=${entity.id}" } }
                }
        }
        return RunResult(events.size, saved, suppressedCount, posted, seed)
    }

    private fun prevValue(target: ReleaseTarget, eventDate: LocalDate): String? = runCatching {
        val history = economicCalendarService.getIndicatorHistory(target.historyCode, target.country) ?: return@runCatching null
        val key = eventDate.toString().replace("-", "")
        val point = history.data
            .mapNotNull { p -> normalizeDateKey(p.date)?.let { it to p } }
            .filter { (k, _) -> k < key }
            .maxByOrNull { (k, _) -> k }?.second ?: return@runCatching null
        EconomicCalendarService.formatEventValue(point.value, history.unit)
    }.onFailure { log.error(it) { "발표 알림 전월 값 조회 실패: ${target.name}" } }.getOrNull()

    /** 이력 포인트 날짜는 YYYY-MM-DD / YYYYMM / YYYYMMDD / YYYYQn 이 섞여 있어 8자리 키로 맞춘다 */
    private fun normalizeDateKey(raw: String): String? {
        val q = Regex("^(\\d{4})Q([1-4])$").find(raw)
        if (q != null) return q.groupValues[1] + "%02d".format((q.groupValues[2].toInt() - 1) * 3 + 1) + "01"
        val digits = raw.filter { it.isDigit() }
        return when (digits.length) { 6 -> digits + "01"; 8 -> digits; else -> null }
    }
}
