package com.example.investfeed.domain.calendar.service

import com.example.investfeed.pressrelease.client.PressReleaseClient
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class OfficialReleaseService(
    private val pressReleaseClient: PressReleaseClient,
) {
    private val log = KotlinLogging.logger {}

    private data class Cached(val value: String?, val fetchedAt: Instant)
    // 확인된 값은 바뀌지 않으므로 계속 쓰고, 미확인은 잠시만 기억해 캘린더 조회마다 피드를 다시 받지 않게 한다
    private val cache = ConcurrentHashMap<String, Cached>()

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val MISS_TTL: Duration = Duration.ofMinutes(5)

        private val BOK_RATE_TITLE = Regex("""^통화정책방향\((\d{4})\.(\d{1,2})\.(\d{1,2})\)""")
        // "기준금리를 현재의 3.00% 수준에서 유지" / "기준금리를 현재의 2.75% 수준에서 3.00% 로 상향 조정"
        private val BOK_RATE_TEXT = Regex("""기준금리를\s*현재의\s*([0-9.]+)\s*%\s*수준에서\s*(?:([0-9.]+)\s*%\s*로\s*(?:상향|하향)\s*조정|유지)""")
        // "2026년 2/4분기 국민소득(잠정)" / "2026년 2/4분기 실질 국내총생산(속보)" / "2025년 4/4분기 및 연간 국민소득(잠정)"
        private val BOK_GDP_TITLE = Regex("""(\d{4})년\s*([1-4])/4분기[^(]*?(?:국민소득\(잠정\)|국내총생산\(속보\))""")
        private val BOK_GDP_TEXT = Regex("""실질\s*국내총생산\(GDP\)은\s*전기대비\s*(-?[0-9.]+)\s*%\s*(성장|감소)""")
        private const val FED_NUM = """[0-9]+(?:-[0-9]+/[0-9]+)?|[0-9]+/[0-9]+"""
        private val FOMC_TEXT = Regex("""target range for the federal funds rate (?:at|to|by [^.]*? to) ($FED_NUM) to ($FED_NUM) percent""")
    }

    /** 금통위 결정 기준금리(예: "3.00"). date 는 결정일 */
    fun findKrRateDecision(date: LocalDate): String? = cached("KR_RATE:$date") {
        val item = pressReleaseClient.getBokPressReleases().firstOrNull { item ->
            BOK_RATE_TITLE.find(item.title)?.groupValues
                ?.let { (_, y, m, d) -> LocalDate.of(y.toInt(), m.toInt(), d.toInt()) } == date
        } ?: return@cached null
        BOK_RATE_TEXT.find(item.description)?.groupValues?.let { it[2].ifEmpty { it[1] } }
            ?: null.also { log.error { "통화정책방향 결정문 해석 실패 ($date): ${item.description.take(200)}" } }
    }

    /** 한국 실질 GDP 전기대비 성장률(예: "0.6", "-0.2"). quarter 는 이벤트 기준 분기("2026Q2"), 모르면 null */
    fun findKrGdp(date: LocalDate, quarter: String?): String? = cached("KR_GDP:$date:$quarter") {
        val item = pressReleaseClient.getBokPressReleases().firstOrNull { item ->
            item.publishedAt.toKstDate() == date && BOK_GDP_TITLE.find(item.title)
                ?.let { m -> quarter == null || "${m.groupValues[1]}Q${m.groupValues[2]}" == quarter } == true
        } ?: return@cached null
        BOK_GDP_TEXT.find(item.description)?.groupValues
            ?.let { (_, rate, dir) -> if (dir == "감소" && !rate.startsWith("-")) "-$rate" else rate }
            ?: null.also { log.error { "GDP 보도자료 해석 실패 ($date): ${item.description.take(200)}" } }
    }

    /** FOMC 목표범위 상단(예: "4", "3.75") — DFEDTARU 와 같은 기준. date 는 KST 발표일 */
    fun findFomcDecision(date: LocalDate): String? = cached("FOMC:$date") {
        val item = pressReleaseClient.getFedMonetaryReleases().firstOrNull { item ->
            item.title.contains("FOMC statement", ignoreCase = true) && item.publishedAt.toKstDate() == date
        } ?: return@cached null
        val text = pressReleaseClient.getFedPageText(item.link)
        FOMC_TEXT.find(text)?.groupValues?.get(2)?.let(::parseFedNumber)
            ?: null.also { log.error { "FOMC 성명 해석 실패 ($date): ${item.link}" } }
    }

    private fun cached(key: String, load: () -> String?): String? {
        cache[key]?.takeIf { it.value != null || it.fetchedAt.isAfter(Instant.now().minus(MISS_TTL)) }?.let { return it.value }
        val value = runCatching(load).onFailure { log.error(it) { "공식 보도자료 확인 실패: $key" } }.getOrNull()
        cache[key] = Cached(value, Instant.now())
        return value
    }

    private fun ZonedDateTime.toKstDate(): LocalDate = withZoneSameInstant(KST).toLocalDate()

    /** "3-3/4" → 3.75, "4" → 4, "1/4" → 0.25 */
    private fun parseFedNumber(raw: String): String? = runCatching {
        raw.split("-")
            .map { part -> if ("/" in part) BigDecimal(part.substringBefore("/")).divide(BigDecimal(part.substringAfter("/"))) else BigDecimal(part) }
            .reduce(BigDecimal::add)
            .stripTrailingZeros().toPlainString()
    }.getOrNull()
}
