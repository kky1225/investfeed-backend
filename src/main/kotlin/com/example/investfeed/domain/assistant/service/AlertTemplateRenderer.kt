package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.assistant.dto.message.*
import com.example.investfeed.domain.assistant.service.TemplateFormat.colored
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredEok
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredRate
import com.example.investfeed.domain.assistant.service.TemplateFormat.eok
import com.example.investfeed.domain.assistant.service.TemplateFormat.monthDay
import com.example.investfeed.domain.assistant.service.TemplateFormat.num
import com.example.investfeed.domain.assistant.service.TemplateFormat.rate
import com.example.investfeed.domain.notification.entity.Direction
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AlertTemplateRenderer {
    const val SUBTYPE_WARN = "INDEX_WARN"
    const val SUBTYPE_CB = "INDEX_CB"
    const val SUBTYPE_RELEASE = "RELEASE"
    const val SUBTYPE_HOLDING = "HOLDING"

    fun indexName(code: String): String = when (code) {
        "KOSPI" -> "코스피"; "KOSDAQ" -> "코스닥"; "NASDAQ" -> "나스닥"; "SP500" -> "S&P500"; else -> code
    }

    fun renderIndexAlert(fact: IndexAlertFact, card: IndexAlertCardFact?): MessageBody {
        val headline = indexHeadline(fact)
        val sections = mutableListOf<Section>()
        card?.flow?.let { sections += flowSection(indexName(fact.quote.indexCode), it) }
        card?.let { usDelaySection(it) }?.let { sections += it }
        return MessageBody(
            type = MessageType.ALERT,
            subtype = if (fact.trigger == AlertTrigger.CB) SUBTYPE_CB else SUBTYPE_WARN,
            asOf = fact.firedAt,
            headline = Headline(headline, HeadlineScope.MARKET, headline),
            summary = card?.let { indexSummary(it) } ?: "",
            sections = sections,
        )
    }

    private fun indexHeadline(f: IndexAlertFact): String {
        val name = indexName(f.quote.indexCode)
        val q = f.quote
        val value = num(q.current, 2)
        if (f.trigger == AlertTrigger.CB) return "$name 서킷브레이커 ${f.stage}단계 ${rate(f.triggerRate, 2)} ($value)"

        val level = f.level ?: return "$name ${rate(f.triggerRate, 2)} ($value)"
        val signed = if (f.direction == AlertDirection.DOWN) -level else level
        return "$name ${rate(signed, 0)} 도달 ($value)"
    }

    private fun indexSummary(card: IndexAlertCardFact): String =
        listOfNotNull(card.primary, card.secondary).joinToString(" · ") {
            "${it.name} ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: num(it.close, 2)} (${num(it.close, 2)})"
        }

    private fun flowSection(marketName: String, f: MarketFlowFact): Section {
        fun cell(v: Long?) = v?.let { coloredEok(it) } ?: "-"
        val lines = listOf(
            "| 주체 | $marketName |\n|---|---|",
            "| 외국인 | ${cell(f.foreign)} |",
            "| 기관 | ${cell(f.institution)} |",
            "| 개인 | ${cell(f.individual)} |",
        )
        return Section(id = "A1", title = "투자자 수급 · 잠정", text = lines.joinToString("\n"))
    }

    private fun usDelaySection(card: IndexAlertCardFact): Section? {
        val notes = listOfNotNull(card.primary?.delayStatus, card.secondary?.delayStatus)
            .distinct().filter { it != "실시간" && it != "장마감" }
        if (notes.isEmpty()) return null
        return Section(id = "A2", title = "미국 지수", text = notes.joinToString(" · ") { "_(시세 $it)_" })
    }

    fun renderRelease(fact: ReleaseFact, target: ReleaseTarget): MessageBody {
        val prev = fact.prevValue?.let { " (${target.prevLabel} $it)" } ?: ""
        val headline = "발표 · ${target.label} ${fact.value}$prev"
        val rows = listOfNotNull(
            "| 발표값 | ${fact.value} |",
            fact.prevValue?.let { "| ${target.prevLabel} | $it |" },
            "| 발표일 | ${monthDay(fact.eventDate)} |",
        )
        val section = Section(
            id = "R1", title = fact.eventName, asOf = fact.detectedAt,
            text = "| 항목 | 값 |\n|---|---:|\n" + rows.joinToString("\n"),
        )
        return MessageBody(
            type = MessageType.ALERT, subtype = SUBTYPE_RELEASE, asOf = fact.detectedAt,
            headline = Headline(headline, HeadlineScope.MARKET, headline),
            summary = "${target.label} ${fact.value}",
            sections = listOf(section),
        )
    }

    private class MergedHit(val main: HoldingAlertHit, val extra52: HoldingAlertHit?) {
        val assetName get() = main.assetName
        val link get() = main.link
    }

    private fun isLimit(d: Direction) = d == Direction.UPPER_LIMIT || d == Direction.LOWER_LIMIT
    private fun is52w(d: Direction) = d == Direction.HIGH_52W || d == Direction.LOW_52W

    private fun mergeHits(hits: List<HoldingAlertHit>): List<MergedHit> =
        hits.groupBy { it.assetCode }.values.map { group ->
            val main = group.firstOrNull { isLimit(it.direction) }
                ?: group.filter { !is52w(it.direction) }.maxByOrNull { it.threshold }
                ?: group.first()
            MergedHit(main, group.firstOrNull { is52w(it.direction) && it !== main })
        }.sortedByDescending { it.main.triggerRate?.let { r -> kotlin.math.abs(r) } ?: -1.0 }   // 등락 폭 큰 순, 52주만인 것은 뒤

    /** 알림함(프론트 formatAlertPrice)과 같은 금액 표기: 미국(_US) 달러 소수 2~4자리, 국내·코인 원 소수 최대 3자리 */
    private fun alertPrice(v: Double, assetCode: String): String =
        if (assetCode.endsWith("_US")) "$" + DecimalFormat("#,##0.00##", DecimalFormatSymbols(Locale.US)).format(v)
        else DecimalFormat("#,##0.###", DecimalFormatSymbols(Locale.US)).format(v) + "원"

    /** 알림함과 같은 문구: "+5% 도달 (72,300원)" · "상한가 도달" · "52주 신고가 달성 (75,000원)" */
    private fun hitLabel(h: HoldingAlertHit, colored: Boolean): String {
        val price = h.price?.let { " (${alertPrice(it, h.assetCode)})" } ?: ""
        fun threshold(signed: Double): String {
            val text = rate(signed, 0)
            return if (colored) colored(text, signed) else text
        }
        return when (h.direction) {
            Direction.UP -> "${threshold(h.threshold)} 도달$price"
            Direction.DOWN -> "${threshold(-h.threshold)} 도달$price"
            Direction.UPPER_LIMIT -> "상한가 도달"
            Direction.LOWER_LIMIT -> "하한가 도달"
            Direction.HIGH_52W -> "52주 신고가 달성$price"
            Direction.LOW_52W -> "52주 신저가 달성$price"
            else -> h.direction.name
        }
    }

    private fun mergedLabel(m: MergedHit, colored: Boolean): String =
        hitLabel(m.main, colored) + (m.extra52?.let { " · ${hitLabel(it, colored)}" } ?: "")

    private fun holdingHeadline(merged: List<MergedHit>): String {
        if (merged.size == 1) {
            val m = merged.first()
            return "보유 ${m.assetName} ${mergedLabel(m, colored = false)}"
        }
        return "보유 ${merged.size}종목 급등락"
    }

    /** 시세 카드 모양용 알림 문구 — 가격은 카드에 크게 따로 보이므로 괄호 금액 없이 ("+5% 도달", "상한가 도달", "52주 신고가 달성") */
    private fun bareLabel(h: HoldingAlertHit): String = when (h.direction) {
        Direction.UP -> "${rate(h.threshold, 0)} 도달"
        Direction.DOWN -> "${rate(-h.threshold, 0)} 도달"
        Direction.UPPER_LIMIT -> "상한가 도달"
        Direction.LOWER_LIMIT -> "하한가 도달"
        Direction.HIGH_52W -> "52주 신고가 달성"
        Direction.LOW_52W -> "52주 신저가 달성"
        else -> h.direction.name
    }

    private fun isUpSide(d: Direction) = d == Direction.UP || d == Direction.UPPER_LIMIT || d == Direction.HIGH_52W

    /**
     * 보유 종목 급등락 — 2026-10-02 잠금 해제: 종목명·등락만 있어 알림함·텔레그램과 같은 수준으로 공개(2차 인증 불필요).
     * 화면은 종목마다 시세 카드 모양(turnCards HOLDING_ALERT)으로 그리고, 섹션 표는 예전 화면용으로 함께 둔다
     */
    fun renderHoldingAlert(hits: List<HoldingAlertHit>, now: LocalDateTime): MessageBody {
        val merged = mergeHits(hits)
        val rows = merged.joinToString("\n") { m -> "| [${m.assetName}](${m.link}) | ${mergedLabel(m, colored = true)} |" }
        val section = Section(
            id = "H1", title = "보유 종목", personal = false, asOf = now,
            text = "| 종목 | 알림 |\n|---|---|\n$rows",
        )
        val cards = merged.map { m ->
            TurnCard(
                kind = "HOLDING_ALERT",
                asOf = now,
                payload = mapOf(
                    "name" to m.assetName,
                    "link" to m.link,
                    "price" to m.main.price,
                    "currency" to if (m.main.assetCode.endsWith("_US")) "USD" else "KRW",
                    "label" to (bareLabel(m.main) + (m.extra52?.let { " · ${bareLabel(it)}" } ?: "")),
                    "up" to isUpSide(m.main.direction),
                ),
            )
        }
        return MessageBody(
            type = MessageType.ALERT, subtype = SUBTYPE_HOLDING, asOf = now,
            headline = Headline(holdingHeadline(merged), HeadlineScope.MARKET),
            sections = listOf(section),
            turnCards = cards,
        )
    }

    // ── 텔레그램 (3단계). 헤드라인 + 요약 2~3줄, HTML, 링크 없음(서버 로컬). 이모지는 여기서만 붙인다 ──

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private fun hhmm(t: LocalDateTime) = t.format(DateTimeFormatter.ofPattern("HH:mm"))

    fun renderIndexAlertTelegram(fact: IndexAlertFact, card: IndexAlertCardFact?): String {
        val emoji = when {
            fact.trigger == AlertTrigger.CB -> "🚨"
            fact.direction == AlertDirection.DOWN -> "📉"
            else -> "📈"
        }
        val lines = mutableListOf("$emoji <b>${esc(indexHeadline(fact))}</b>")
        // 둘째 줄은 발동 지수가 아닌 쪽 (국내: 코스피200·코스닥150, 미국: 나스닥↔S&P500). 헤드라인과 같은 지수는 반복하지 않는다
        val firedName = indexName(fact.quote.indexCode)
        listOfNotNull(card?.secondary, card?.primary).firstOrNull { it.name != firedName }?.let { s ->
            lines += esc("${s.name} ${s.changeRate?.let { r -> rate(r, 2) } ?: ""} (${num(s.close, 2)})".replace("  ", " "))
        }
        card?.flow?.let { f ->
            fun cell(v: Long?) = v?.let { eok(it) } ?: "-"
            lines += esc("수급(잠정) 외국인 ${cell(f.foreign)} · 기관 ${cell(f.institution)} · 개인 ${cell(f.individual)}")
        }
        card?.let { c ->
            listOfNotNull(c.primary?.delayStatus, c.secondary?.delayStatus)
                .distinct().filter { it != "실시간" && it != "장마감" }
                .takeIf { it.isNotEmpty() }?.let { lines += esc("시세 ${it.joinToString(" · ")}") }
        }
        return lines.joinToString("\n")
    }

    fun renderReleaseTelegram(fact: ReleaseFact, target: ReleaseTarget): String {
        val prev = fact.prevValue?.let { " (${target.prevLabel} $it)" } ?: ""
        return listOf(
            "📊 <b>${esc("발표 · ${target.label} ${fact.value}$prev")}</b>",
            esc(fact.eventName),
            esc("발표일 ${monthDay(fact.eventDate)} · ${hhmm(fact.detectedAt)} 확인"),
        ).joinToString("\n")
    }

    fun renderHoldingAlertTelegram(hits: List<HoldingAlertHit>): String {
        val merged = mergeHits(hits)
        val lines = mutableListOf("🔔 <b>${esc(holdingHeadline(merged))}</b>")
        if (merged.size > 1) merged.forEach { m -> lines += esc("${m.assetName} ${mergedLabel(m, colored = false)}") }
        return lines.joinToString("\n")
    }
}
