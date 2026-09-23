package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.assistant.dto.message.*
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredEok
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredRate
import com.example.investfeed.domain.assistant.service.TemplateFormat.monthDay
import com.example.investfeed.domain.assistant.service.TemplateFormat.num
import com.example.investfeed.domain.assistant.service.TemplateFormat.rate

object AlertTemplateRenderer {
    const val SUBTYPE_WARN = "INDEX_WARN"
    const val SUBTYPE_CB = "INDEX_CB"
    const val SUBTYPE_RELEASE = "RELEASE"

    private const val RETREAT_GAP = 0.2

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

        val retreated = when (f.direction) {
            AlertDirection.DOWN -> q.currentRate - f.triggerRate >= RETREAT_GAP
            AlertDirection.UP -> f.triggerRate - q.currentRate >= RETREAT_GAP
        }
        return if (retreated) "$name 장중 ${rate(f.triggerRate, 2)} 도달 · 현재 ${rate(q.currentRate, 2)} ($value)"
        else "$name ${rate(f.triggerRate, 2)} ($value)"
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
}
