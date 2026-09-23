package com.example.investfeed.domain.assistant.service

import com.example.investfeed.domain.assistant.dto.factsheet.*
import com.example.investfeed.domain.assistant.dto.message.*
import com.example.investfeed.domain.assistant.service.TemplateFormat.colored
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredBp
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredEok
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredRate
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredUsd
import com.example.investfeed.domain.assistant.service.TemplateFormat.coloredWon
import com.example.investfeed.domain.assistant.service.TemplateFormat.monthDay
import com.example.investfeed.domain.assistant.service.TemplateFormat.num
import com.example.investfeed.domain.assistant.service.TemplateFormat.usd
import com.example.investfeed.domain.assistant.service.TemplateFormat.won
import kotlin.math.abs

object BriefingTemplateRenderer {
    private const val MAX_ROWS = 15

    data class Rendered(val summary: String, val sections: List<Section>)

    fun renderKrClose(sheet: KrCloseFactSheet): Rendered {
        val sections = mutableListOf<Section>()
        sections += krIndexSection(sheet)
        sections += flowSection(sheet.flow)
        sections += sectorSection(sheet.sectors)
        sections += krFxSection(sheet.usdKrw)
        return Rendered(summary = krCloseSummary(sheet), sections = sections)
    }

    private fun krCloseSummary(s: KrCloseFactSheet): String =
        listOfNotNull(s.kospi, s.kospi200, s.kosdaq, s.kosdaq150).joinToString(" · ") {
            "${it.name} ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: num(it.close, 2)} (${num(it.close, 2)})"
        }

    private fun krIndexSection(s: KrCloseFactSheet): Section {
        val rows = listOfNotNull(
            s.kospi?.let { tradeRow(it, s.kospiRate5d) },
            s.kosdaq?.let { tradeRow(it, s.kosdaqRate5d) },
        )
        if (rows.isEmpty()) return failedSection("C2", "거래대금 · 5일 등락")
        return okSection("C2", "거래대금 · 5일 등락", "| 지수 | 거래대금 | 5일 등락 |\n|---|---|---|\n" + rows.joinToString("\n"))
    }

    private fun tradeRow(k: IndexFact, rate5d: Double?): String {
        val amount = k.tradeAmount?.let { "${num(it / 1_000_000.0, 1)}조" } ?: "-"
        return "| ${k.name} | $amount | ${rate5d?.let { coloredRate(it) } ?: "-"} |"
    }

    private fun flowSection(f: FlowFact?): Section {
        f ?: return failedSection("C3", "투자자 수급")
        val kospi = f.kospi
        val kosdaq = f.kosdaq
        if (kospi == null && kosdaq == null) return failedSection("C3", "투자자 수급")
        fun cell(v: Long?) = v?.let { coloredEok(it) } ?: "-"
        val lines = mutableListOf("| 주체 | 코스피 | 코스닥 |\n|---|---|---|")
        lines += "| 외국인 | ${cell(kospi?.foreign)} | ${cell(kosdaq?.foreign)} |"
        lines += "| 기관 | ${cell(kospi?.institution)} | ${cell(kosdaq?.institution)} |"
        lines += "| 개인 | ${cell(kospi?.individual)} | ${cell(kosdaq?.individual)} |"
        return okSection("C3", "투자자 수급 · 잠정", lines.joinToString("\n"))
    }

    private fun sectorSection(list: List<SectorFact>?): Section {
        list ?: return failedSection("C4", "업종")
        if (list.isEmpty()) return emptySection("C4", "업종")
        val sorted = list.sortedByDescending { it.changeRate }
        val table = buildString {
            append("| 업종 | 등락 | 거래대금 |\n|---|---|---|\n")
            sorted.forEach { append("| ${it.name} | ${coloredRate(it.changeRate)} | ${it.tradeAmount?.let { a -> "${num(a / 100.0, 0)}억" } ?: "-"} |\n") }
        }
        return okSection("C4", "업종", table.trimEnd())
    }

    private fun krFxSection(q: QuoteFact?): Section {
        q ?: return failedSection("C6", "환율 마감")
        return okSection("C6", "환율 마감 · 하나은행 고시", "| 통화 | 마감 | 등락 |\n|---|---|---|\n| 원/달러 | ${num(q.price, 1)} | ${q.changeRate?.let { coloredRate(it) } ?: "-"} |")
    }

    fun renderKrPre(sheet: KrPreFactSheet): Rendered {
        val sections = mutableListOf<Section>()
        if (!sheet.krHoliday) sections += recommendSection(sheet.recommend)
        sections += macroSection(sheet.macro)
        return Rendered(summary = krPreSummary(sheet), sections = sections)
    }

    private fun recommendSection(list: List<RecommendFact>?): Section {
        list ?: return failedSection("P2B", "오늘 추천")
        if (list.isEmpty()) return okSection("P2B", "오늘 추천", "오늘 추천 산출 없음")
        val rows = list.joinToString("\n") { r -> "| ${TemplateFormat.coloredGrade(r.grade)} | ${r.names.size} | ${r.names.joinToString(", ")} |" }
        return okSection("P2B", "오늘 추천 · 시스템 분류", "| 등급 | 종목 수 | 종목 |\n|:---|---:|:---|\n$rows")
    }

    private fun krPreSummary(s: KrPreFactSheet): String {
        val k = s.krPrev ?: return ""
        return listOfNotNull(k.kospi, k.kospi200, k.kosdaq, k.kosdaq150).joinToString(" · ") {
            "${it.name} ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: num(it.close, 2)} (${num(it.close, 2)})"
        }
    }

    private fun macroSection(m: MacroFact?): Section {
        m ?: return failedSection("P4", "매크로")
        val rows = listOfNotNull(
            m.usdKrw?.let { "| 원/달러 | ${num(it.price, 1)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
            m.dollarIndex?.let { "| 달러인덱스 | ${num(it.price, 2)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
            m.gold?.let { "| 금 | ${num(it.price, 1)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
            m.wti?.let { "| WTI | ${num(it.price, 2)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
        )
        if (rows.isEmpty()) return failedSection("P4", "매크로")
        return okSection("P4", "매크로", "| 항목 | 값 | 등락 |\n|---|---|---|\n" + rows.joinToString("\n"))
    }

    fun renderKrHoldings(personal: PersonalFactSheet?): Rendered =
        Rendered(summary = "", sections = listOfNotNull(personal?.let { myKrHoldingsSection(it) }))

    fun renderCoinDaily(sheet: CoinDailyFactSheet, personal: PersonalFactSheet?): Rendered {
        val sections = mutableListOf<Section>()
        personal?.let { p -> myCoinsSection(p)?.let { sections += it } }
        sections += coinMarket(sheet)
        return Rendered(summary = coinSummary(sheet), sections = sections)
    }

    private fun coinSummary(s: CoinDailyFactSheet): String = listOfNotNull(
        s.btc?.let { "BTC ${it.changeRate?.let { r -> coloredRate(r) } ?: num(it.close, 0)} (${num(it.close, 0)})" },
        s.eth?.let { "ETH ${it.changeRate?.let { r -> coloredRate(r) } ?: num(it.close, 0)} (${num(it.close, 0)})" },
        s.fearGreed?.let { "공포탐욕 $it (${TemplateFormat.fearGreedLabel(it)})" },
    ).joinToString(" · ")

    private fun coinMarket(s: CoinDailyFactSheet): Section {
        val rows = listOfNotNull(s.btc, s.eth).map { c ->
            "| ${c.name} | ${num(c.close, 0)} | ${c.changeRate?.let { coloredRate(it) } ?: "-"} | ${c.tradeAmount?.let { "${num(it / 1e8, 0)}억" } ?: "-"} |"
        }
        if (rows.isEmpty()) return failedSection("K2", "코인 시세 · 업비트 09:00 마감")
        return okSection("K2", "코인 시세 · 업비트 09:00 마감", "| 코인 | 종가 | 등락 | 거래대금 |\n|---|---|---|---|\n" + rows.joinToString("\n"))
    }

    fun renderUsClose(sheet: UsCloseFactSheet, personal: PersonalFactSheet?): Rendered {
        val sections = mutableListOf<Section>()
        personal?.let { p -> myUsHoldingsSection(p)?.let { sections += it } }
        usIndexSection(sheet.indexes, sheet.earlyClose)?.let { sections += it }
        sections += treasurySection(sheet.treasury)
        sections += usFxSection(sheet.usdKrw)
        return Rendered(summary = usCloseSummary(sheet), sections = sections)
    }

    private fun usCloseSummary(s: UsCloseFactSheet): String =
        s.indexes.orEmpty().joinToString(" · ") { i ->
            if (i.name == "VIX") "VIX ${num(i.close, 2)}" + (i.changeRate?.let { " (${coloredRate(it, 2)})" } ?: "")
            else "${i.name} ${i.changeRate?.let { coloredRate(it, 2) } ?: num(i.close, 2)} (${num(i.close, 2)})"
        }

    private fun usIndexSection(list: List<IndexFact>?, earlyClose: Boolean): Section? {
        list ?: return failedSection("U2", "미국 지수")
        val vix = list.firstOrNull { it.name == "VIX" }?.close
        val notes = mutableListOf<String>()
        if (vix != null && vix >= 30) notes += "VIX ${num(vix, 1)} 변동성 고조" else if (vix != null && vix >= 25) notes += "VIX ${num(vix, 1)} 경계"
        // 카드는 마감+10분(16:10 ET)에 나가지만 VIX 종가는 마감+15분(16:15 ET)에 확정된다 (2026-09-22 결정: 발행 시각은 그대로, 기준 시각만 표기)
        if (vix != null) notes += if (earlyClose) "_(VIX 13:10 ET 시점 값, 종가 확정 13:15 ET)_" else "_(VIX 16:10 ET 시점 값, 종가 확정 16:15 ET)_"
        list.mapNotNull { it.delayStatus }.distinct().filter { it != "실시간" && it != "장마감" }.forEach { notes += "_(시세 ${it})_" }
        if (notes.isEmpty()) return null
        return okSection("U2", "미국 지수", notes.joinToString(" · "))
    }

    private fun treasurySection(t: TreasuryFact?): Section {
        t ?: return failedSection("U2B", "미국 국채 금리")
        val rows = listOfNotNull(
            t.y10?.let { "| 10년물 | ${num(it, 2)}% | ${t.y10Bp?.let { b -> coloredBp(b) } ?: "-"} |" },
            t.y2?.let { "| 2년물 | ${num(it, 2)}% | ${t.y2Bp?.let { b -> coloredBp(b) } ?: "-"} |" },
            t.spreadBp?.let { "| 스프레드 | ${colored("${TemplateFormat.sign(it.toDouble())}${abs(it)}bp", it.toDouble())}${if (it < 0) " (역전)" else ""} | - |" },
        )
        if (rows.isEmpty()) return failedSection("U2B", "미국 국채 금리")
        val parts = listOf("| 만기 | 수익률 | 변화 |\n|---|---|---|\n" + rows.joinToString("\n"))
        val suffix = when {
            t.stale -> " · ${if (t.source == UsIndexDailyService.SOURCE_FRED) "FRED " else ""}${monthDay(t.asOfDate)} 마감 기준 (당일 값 미수신)"
            t.source == UsIndexDailyService.SOURCE_FRED -> " · FRED"
            else -> ""
        }
        return okSection("U2B", "미국 국채 금리$suffix", parts.first())
    }

    private fun usFxSection(q: QuoteFact?): Section {
        q ?: return failedSection("U4", "환율")
        return okSection("U4", "환율 · 전일 서울 고시", "| 통화 | 값 | 등락 |\n|---|---|---|\n| 원/달러 | ${num(q.price, 1)} | ${q.changeRate?.let { coloredRate(it) } ?: "-"} |")
    }

    private fun myKrHoldingsSection(p: PersonalFactSheet): Section? {
        val brokers = p.krBrokers ?: return failedSection("C1A", "내 국내 종목", personal = true)
        val ok = brokers.filter { !it.failed }
        if (brokers.isNotEmpty() && ok.isEmpty()) return failedSection("C1A", "내 국내 종목", personal = true)
        if (brokers.isEmpty() || ok.all { it.items.isEmpty() }) return null          // 보유 없으면 섹션 생략
        val realizedBy = p.realized?.byBroker?.toMap().orEmpty()
        val block = accountBlock(
            brokers = brokers, krw = true,
            total = won(Math.round(ok.sumOf { it.eval })),
            dayChange = p.krDayChangeWon()?.toDouble(), dayRate = p.krAssetDayRate(),
            realizedTotal = p.realized?.monthTotalWon,
            realizedOf = { realizedBy[it] },
        )
        return okSection("C1A", "내 국내 종목", "", personal = true, account = block)
    }

    private fun myUsHoldingsSection(p: PersonalFactSheet): Section? {
        val brokers = p.usBrokers ?: return failedSection("U1A", "내 미국 종목", personal = true)
        val ok = brokers.filter { !it.failed }
        if (brokers.isNotEmpty() && ok.isEmpty()) return failedSection("U1A", "내 미국 종목", personal = true)
        if (brokers.isEmpty() || ok.all { it.items.isEmpty() }) return null
        val evalUsd = ok.sumOf { it.eval }
        val block = accountBlock(
            brokers = brokers, krw = false,
            total = usd(evalUsd) + (p.usdKrw?.let { " (${won(Math.round(evalUsd * it))})" } ?: ""),
            dayChange = p.usDayChangeUsd(), dayRate = p.usAssetDayRate(),
        )
        return okSection("U1A", "내 미국 종목", "", personal = true, account = block)
    }

    private fun myCoinsSection(p: PersonalFactSheet): Section? {
        val exchanges = p.coinExchanges ?: return failedSection("K1A", "내 코인", personal = true)
        val ok = exchanges.filter { !it.failed }
        if (exchanges.isNotEmpty() && ok.isEmpty()) return failedSection("K1A", "내 코인", personal = true)
        if (ok.all { it.items.isEmpty() }) return null
        val eval = ok.sumOf { it.eval }
        val dayChange = ok.mapNotNull { it.dayChange }.takeIf { it.isNotEmpty() }?.sum()
        val dayRate = dayChange?.let { c -> (eval - c).takeIf { it > 0 }?.let { c / it * 100 } }
        // 현재가·일간은 09:00 일봉 기준. 열은 코인 계좌 화면(CryptoHoldingList) 순서
        val block = accountBlock(
            brokers = exchanges.filter { it.failed || it.items.isNotEmpty() }, krw = true,
            total = won(Math.round(eval)), dayChange = dayChange, dayRate = dayRate, nameHeader = "코인명",
        )
        return okSection("K1A", "내 코인", "", personal = true, account = block)
    }

    /** 총액 카드 + 증권사 카드. 칸 순서는 총 수익 → 일간 수익 → 실현(이달). 증권사가 하나면 증권사 카드의 칸은 비운다 */
    private fun accountBlock(
        brokers: List<BrokerHoldingsFact>, krw: Boolean, total: String,
        dayChange: Double?, dayRate: Double?,
        realizedTotal: Long? = null, realizedOf: (String) -> Long? = { null },
        nameHeader: String = "종목명",
    ): AccountBlock {
        val ok = brokers.filter { !it.failed }
        fun signed(v: Double) = if (krw) coloredWon(Math.round(v)) else coloredUsd(v)
        fun withRate(amount: Double?, rate: Double?): String? = when {
            amount != null -> signed(amount) + (rate?.let { " (${coloredRate(it)})" } ?: "")
            rate != null -> coloredRate(rate)
            else -> null
        }
        fun profitOf(bs: List<BrokerHoldingsFact>) = bs.flatMap { it.items }.mapNotNull { it.evalProfit }.takeIf { it.isNotEmpty() }?.sum()

        val stats = listOfNotNull(
            withRate(profitOf(ok), weightedTotalRate(ok))?.let { AccountStat("총 수익", it) },
            withRate(dayChange, dayRate)?.let { AccountStat("일간 수익", it) },
            realizedTotal?.let { AccountStat("실현 (이달)", coloredWon(it)) },
        )
        val single = brokers.size == 1
        val cards = brokers.map { b ->
            if (b.failed) AccountBroker(name = b.brokerName, total = null, failed = true)
            else AccountBroker(
                name = b.brokerName,
                total = if (krw) won(Math.round(b.eval)) else usd(b.eval),
                stats = if (single) emptyList() else listOfNotNull(
                    withRate(profitOf(listOf(b)), b.totalRate)?.let { AccountStat("총 수익", it) },
                    withRate(b.dayChange, b.dayRate)?.let { AccountStat("일간 수익", it) },
                    realizedOf(b.brokerName)?.let { AccountStat("실현 (이달)", coloredWon(it)) },
                ),
                table = stockTable(b, krw, nameHeader),
            )
        }
        return AccountBlock(total = total, stats = stats, brokers = cards)
    }

    private fun stockTable(b: BrokerHoldingsFact, krw: Boolean, nameHeader: String): String {
        fun money(v: Double?) = v?.let { if (krw) num(Math.round(it)) else usd(it) } ?: "-"
        fun profit(v: Double?) = v?.let { TemplateFormat.colored(TemplateFormat.sign(it) + (if (krw) num(abs(Math.round(it))) else usd(it)), it) } ?: "-"   // 표 폭 때문에 단위 없이
        fun rate(v: Double?) = v?.let { coloredRate(it) } ?: "-"
        val rows = b.items.take(MAX_ROWS).map { h ->
            val name = h.link?.let { "[${h.name}]($it)" } ?: h.name
            "| $name | ${rate(h.totalRate)} | ${money(h.curPrc)} | ${money(h.eval)} | ${profit(h.evalProfit)} | ${rate(h.dayRate)} |"
        }
        val more = if (b.items.size > MAX_ROWS) listOf("| 외 ${b.items.size - MAX_ROWS}종목 | | | | | |") else emptyList()
        return (listOf("| $nameHeader | 수익률 | 현재가 | 평가금액 | 평가손익 | 일간 |", "|:---|---:|---:|---:|---:|---:|") + rows + more).joinToString("\n")
    }

    private fun weightedTotalRate(brokers: List<BrokerHoldingsFact>): Double? {
        val eval = brokers.sumOf { it.eval }
        if (eval <= 0) return null
        val rated = brokers.filter { it.totalRate != null }
        if (rated.isEmpty()) return null
        return rated.sumOf { it.eval * it.totalRate!! } / eval
    }

    // ═══════════════════════ helpers ═══════════════════════

    private fun okSection(id: String, title: String, text: String, personal: Boolean = false, summary: String? = null, account: AccountBlock? = null) =
        Section(id = id, title = title, personal = personal, status = SectionStatus.OK, text = text, summary = summary, account = account)

    private fun failedSection(id: String, title: String, personal: Boolean = false) =
        Section(id = id, title = title, personal = personal, status = SectionStatus.FAILED, text = "")

    private fun emptySection(id: String, title: String, personal: Boolean = false) =
        Section(id = id, title = title, personal = personal, status = SectionStatus.EMPTY, text = "")
}
