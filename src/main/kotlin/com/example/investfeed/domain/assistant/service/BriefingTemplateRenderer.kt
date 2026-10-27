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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object BriefingTemplateRenderer {
    private const val MAX_ROWS = 15
    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    data class Rendered(val summary: String, val sections: List<Section>)

    fun renderKrClose(sheet: KrCloseFactSheet): Rendered {
        val sections = mutableListOf<Section>()
        sections += krIndexSection(sheet)
        sections += flowSection(sheet.flow)
        sections += sectorSection(sheet.sectors)
        sections += krTreasurySection(sheet.krTreasury)
        sections += krFxSection(sheet.usdKrw)
        return Rendered(summary = krCloseSummary(sheet), sections = sections)
    }

    private fun krCloseSummary(s: KrCloseFactSheet): String =
        listOfNotNull(s.kospi, s.kospi200, s.kosdaq, s.kosdaq150).joinToString(" · ") {
            "${it.name} ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: num(it.close, 2)} (${num(it.close, 2)})"
        }

    /**
     * 서킷브레이커가 발동한 지수 이름 옆 경고 표기 `{!|툴팁}` — 지수 표(C2)에만 붙인다. 프론트가 경고 아이콘 + 툴팁으로 그림 (종목 상세 투자경고 아이콘과 같은 모양).
     * 툴팁은 단계별 한 줄씩 "1단계 12:38~13:08", 3단계는 당일 장 종료라 "3단계 14:40~장 종료"
     */
    private fun cbWarn(name: String, list: List<CircuitBreakerFact>): String {
        val mine = list.filter { it.name == name }.sortedBy { it.stage }
        if (mine.isEmpty()) return ""
        val lines = mine.map { cb -> "${cb.stage}단계 ${cb.firedAt.format(HHMM)}~${cb.resumeAt?.format(HHMM) ?: "장 종료"}" }
        return "{!|${(listOf("서킷브레이커") + lines).joinToString("\n")}}"
    }

    private fun krIndexSection(s: KrCloseFactSheet): Section {
        val rows = listOfNotNull(
            s.kospi?.let { tradeRow(it, s.kospiRate5d, cbWarn(it.name, s.circuitBreakers)) },
            s.kosdaq?.let { tradeRow(it, s.kosdaqRate5d, cbWarn(it.name, s.circuitBreakers)) },
        )
        if (rows.isEmpty()) return failedSection("C2", "거래대금 · 5일 등락")
        return okSection("C2", "거래대금 · 5일 등락", "| 지수 | 거래대금 | 5일 등락 |\n|---|---|---|\n" + rows.joinToString("\n"))
    }

    private fun tradeRow(k: IndexFact, rate5d: Double?, warn: String = ""): String {
        val amount = k.tradeAmount?.let { "${num(it / 1_000_000.0, 1)}조" } ?: "-"
        return "| ${k.name}$warn | $amount | ${rate5d?.let { coloredRate(it) } ?: "-"} |"
    }

    // provisional: 장중·마감 직후 수급은 잠정치 (20:00 이후·지난 날짜는 확정치)
    private fun flowSection(f: FlowFact?, withKospi: Boolean = true, withKosdaq: Boolean = true, provisional: Boolean = true): Section {
        f ?: return failedSection("C3", "투자자 수급")
        val cols = listOfNotNull(("코스피" to f.kospi).takeIf { withKospi }, ("코스닥" to f.kosdaq).takeIf { withKosdaq })
        if (cols.all { it.second == null }) return failedSection("C3", "투자자 수급")
        fun cell(v: Long?) = v?.let { coloredEok(it) } ?: "-"
        val lines = mutableListOf("| 주체 | ${cols.joinToString(" | ") { it.first }} |\n|---|${cols.joinToString("") { "---|" }}")
        lines += "| 외국인 | ${cols.joinToString(" | ") { cell(it.second?.foreign) }} |"
        lines += "| 기관 | ${cols.joinToString(" | ") { cell(it.second?.institution) }} |"
        lines += "| 개인 | ${cols.joinToString(" | ") { cell(it.second?.individual) }} |"
        return okSection("C3", if (provisional) "투자자 수급 · 잠정" else "투자자 수급", lines.joinToString("\n"))
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

    private fun krTreasurySection(t: KrTreasuryFact?): Section {
        val list = t?.yields.orEmpty()
        if (list.isEmpty()) return failedSection("C7", "한국 국채 금리")
        val fallbackDates = list.mapNotNull { it.fallbackDate }.distinct()
        val allFallback = list.all { it.fallbackDate != null } && fallbackDates.size == 1
        val rows = list.sortedByDescending { it.name.removeSuffix("년").toIntOrNull() ?: 0 }
            .map { y ->
                val note = y.fallbackDate?.takeIf { !allFallback }?.let { " _(ECOS ${monthDay(it)} 종가)_" }
                    ?: y.staleAt?.let { " _(${monthDay(it.toLocalDate())} ${it.format(HHMM)} 기준)_" } ?: ""
                "| ${y.name}물 | ${num(y.yield, 2)}%$note | ${y.changeBp?.let { b -> coloredBp(b) } ?: "-"} |"
            }
        val y10 = list.firstOrNull { it.name == "10년" }
        val y3 = list.firstOrNull { it.name == "3년" }
        val spread = if (y10 != null && y3 != null && y10.fallbackDate == y3.fallbackDate) Math.round((y10.yield - y3.yield) * 100).toInt() else null
        val spreadRow = spread?.let { listOf("| 스프레드(10-3년) | ${colored("${TemplateFormat.sign(it.toDouble())}${abs(it)}bp", it.toDouble())}${if (it < 0) " (역전)" else ""} | - |") }.orEmpty()
        val suffix = if (allFallback) " · ECOS ${monthDay(fallbackDates.first())} 종가 기준 (당일 값 미수신)" else ""
        return okSection("C7", "한국 국채 금리$suffix", "| 만기 | 수익률 | 변화 |\n|---|---|---|\n" + (rows + spreadRow).joinToString("\n"))
    }

    private fun krFxSection(q: QuoteFact?): Section {
        q ?: return failedSection("C6", "환율 마감")
        return okSection("C6", "환율 마감 · 하나은행 고시", "| 통화 | 마감 | 등락 |\n|---|---|---|\n| 원/달러 | ${num(q.price, 1)}${staleNote(q)} | ${q.changeRate?.let { coloredRate(it, 2) } ?: "-"} |")
    }

    /** 오래된 시세(수집 실패로 갱신 안 된 값)면 값 옆에 "10/16(금) 15:52 기준"을 붙인다 */
    private fun staleNote(q: QuoteFact): String =
        q.staleAt?.let { " _(${monthDay(it.toLocalDate())} ${it.format(HHMM)} 기준)_" } ?: ""

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
        return okSection("P2B", "오늘 추천", "| 등급 | 종목 수 | 종목 |\n|:---|---:|:---|\n$rows")
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
            m.usdKrw?.let { "| 원/달러 | ${num(it.price, 1)}${staleNote(it)} | ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: "-"} |" },
            m.dollarIndex?.let { "| 달러인덱스 | ${num(it.price, 2)}${staleNote(it)} | ${it.changeRate?.let { r -> coloredRate(r, 2) } ?: "-"} |" },
            m.gold?.let { "| 금 | ${num(it.price, 1)}${staleNote(it)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
            m.wti?.let { "| WTI | ${num(it.price, 2)}${staleNote(it)} | ${it.changeRate?.let { r -> coloredRate(r) } ?: "-"} |" },
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

    /** 비서 채팅 국내 지수 카드. 지난 날짜에 수급 저장분이 없으면(flowRequired=false) 수급 칸을 뺀다 */
    fun renderKrMarketNow(sheet: KrCloseFactSheet, kospi: Boolean = true, kosdaq: Boolean = true, provisional: Boolean = true, flowRequired: Boolean = true): Rendered =
        Rendered(
            summary = krCloseSummary(sheet),
            sections = if (sheet.flow == null && !flowRequired) emptyList() else listOf(flowSection(sheet.flow, kospi, kosdaq, provisional)),
        )

    fun renderTreasury(kr: KrTreasuryFact?, us: TreasuryFact?, krNotice: String?, usNotice: String?, withKr: Boolean, withUs: Boolean, krTitleNote: String? = null): Rendered {
        val sections = mutableListOf<Section>()
        if (withKr) sections += krNotice?.let { okSection("C7", "한국 국채 금리", it) }
            ?: krTreasurySection(kr).let { sec -> krTitleNote?.let { sec.copy(title = "${sec.title} · $it") } ?: sec }
        if (withUs) sections += usNotice?.let { okSection("U2B", "미국 국채 금리", it) } ?: treasurySection(us)
        return Rendered(summary = "", sections = sections)
    }

    /**
     * 비서 환율 카드: 원/달러·달러인덱스 타일(등락률 · 값) + 표(전일 대비).
     * 환율은 하루 변동이 작아 등락률·전일 대비 모두 소수 2자리 (1자리면 +0.03% 가 0.0% 로 보임)
     */
    fun renderExchangeRates(usdKrw: QuoteFact?, dollarIndex: QuoteFact?): Rendered {
        val items = listOfNotNull(usdKrw?.let { "원/달러" to it }, dollarIndex?.let { "달러인덱스" to it })
        val summary = items.joinToString(" · ") { (name, q) ->
            "$name ${q.changeRate?.let { coloredRate(it, 2) } ?: num(q.price, 2)} (${num(q.price, 2)})"
        }
        val rows = items.joinToString("\n") { (name, q) ->
            "| $name | ${num(q.price, 2)}${staleNote(q)} | ${q.changeAmount?.let { colored("${TemplateFormat.sign(it)}${num(abs(it), 2)}", it) } ?: "-"} |"
        }
        val section = if (items.isEmpty()) failedSection("FX", "환율")
            else okSection("FX", "환율", "| 구분 | 값 | 전일 대비 |\n|---|---|---|\n$rows")
        return Rendered(summary = summary, sections = listOf(section))
    }

    fun renderUsMarketNow(sheet: UsCloseFactSheet): Rendered =
        Rendered(summary = usCloseSummary(sheet), sections = listOf(treasurySection(sheet.treasury), usFxSection(sheet.usdKrw, "환율")))

    fun renderUsClose(sheet: UsCloseFactSheet, personal: PersonalFactSheet?): Rendered {
        val sections = mutableListOf<Section>()
        personal?.let { p -> myUsHoldingsSection(p)?.let { sections += it } }
        usIndexSection(sheet.indexes)?.let { sections += it }
        sections += treasurySection(sheet.treasury)
        sections += usFxSection(sheet.usdKrw)
        return Rendered(summary = usCloseSummary(sheet), sections = sections)
    }

    private fun usCloseSummary(s: UsCloseFactSheet): String =
        s.indexes.orEmpty().joinToString(" · ") { i ->
            "${i.name} ${i.changeRate?.let { coloredRate(it, 2) } ?: num(i.close, 2)} (${num(i.close, 2)})"
        }

    private fun usIndexSection(list: List<IndexFact>?): Section? {
        list ?: return failedSection("U2", "미국 지수")
        val notes = mutableListOf<String>()
        list.mapNotNull { it.delayStatus }.distinct().filter { it != "실시간" && it != "장마감" }.forEach { notes += "_(시세 ${it})_" }
        if (notes.isEmpty()) return null
        return okSection("U2", "미국 지수", notes.joinToString(" · "))
    }

    private fun treasurySection(t: TreasuryFact?): Section {
        t ?: return failedSection("U2B", "미국 국채 금리")
        // 채권 휴장일엔 직전 거래일 값만 보이고 변화는 "-" (그날 움직임이 아님)
        fun bp(v: Double?) = v?.takeUnless { t.bondHoliday }?.let { coloredBp(it) } ?: "-"
        val rows = listOfNotNull(
            t.y30?.let { "| 30년물 | ${num(it, 2)}% | ${bp(t.y30Bp)} |" },
            t.y10?.let { "| 10년물 | ${num(it, 2)}% | ${bp(t.y10Bp)} |" },
            t.y5?.let { "| 5년물 | ${num(it, 2)}% | ${bp(t.y5Bp)} |" },
            t.y2?.let { "| 2년물 | ${num(it, 2)}% | ${bp(t.y2Bp)} |" },
            t.spreadBp?.let { "| 스프레드(10-2년) | ${colored("${TemplateFormat.sign(it.toDouble())}${abs(it)}bp", it.toDouble())}${if (it < 0) " (역전)" else ""} | - |" },
        )
        if (rows.isEmpty()) return failedSection("U2B", "미국 국채 금리")
        val parts = listOf("| 만기 | 수익률 | 변화 |\n|---|---|---|\n" + rows.joinToString("\n"))
        val suffix = when {
            t.bondHoliday -> " · 채권시장 휴장 (${monthDay(t.asOfDate)} 마감 기준)"
            t.stale -> " · ${if (t.source == UsTreasuryService.SOURCE_FRED) "FRED " else ""}${monthDay(t.asOfDate)} 마감 기준 (당일 값 미수신)"
            t.source == UsTreasuryService.SOURCE_FRED -> " · FRED"
            else -> ""
        }
        return okSection("U2B", "미국 국채 금리$suffix", parts.first())
    }

    // 환율 등락률은 하루 변동이 작아 소수 2자리 (1자리면 +0.03% 가 0.0% 로 보임, 2026-10-26). 국내 마감·장전 매크로도 같음
    private fun usFxSection(q: QuoteFact?, title: String = "환율 · 전일 서울 고시"): Section {
        q ?: return failedSection("U4", "환율")
        return okSection("U4", title, "| 통화 | 값 | 등락 |\n|---|---|---|\n| 원/달러 | ${num(q.price, 1)}${staleNote(q)} | ${q.changeRate?.let { coloredRate(it, 2) } ?: "-"} |")
    }

    /**
     * 비서 "내 계좌" 카드용 — 계좌(증권사)마다 섹션 1개 (2026-10-01).
     * 모양은 브리핑 계좌 섹션과 같다: 총 평가금액(원화 환산) + 총 수익·일간 수익·실현(이달) 칸, 그 아래 국내·해외·코인 카드와 같은 종목 표
     */
    fun accountSectionsByBroker(p: PersonalFactSheet): List<Section> {
        data class Part(val label: String, val fact: BrokerHoldingsFact, val krw: Boolean, val nameHeader: String)
        val fx = p.usdKrw
        val realizedBy = p.realized?.byBroker?.toMap().orEmpty()
        val parts = p.krBrokers.orEmpty().map { Part("국내", it, true, "종목명") } +
            p.usBrokers.orEmpty().map { Part("해외", it, false, "종목명") } +
            p.coinExchanges.orEmpty().map { Part("코인", it, true, "코인명") }
        fun toKrw(v: Double, part: Part): Double? = if (part.krw) v else fx?.let { v * it }
        fun withRate(amount: Double?, rate: Double?): String? = when {
            amount != null -> coloredWon(Math.round(amount)) + (rate?.let { " (${coloredRate(it)})" } ?: "")
            rate != null -> coloredRate(rate)
            else -> null
        }
        return parts.groupBy { it.fact.brokerName }.entries.mapIndexedNotNull { i, (broker, ps) ->
            val id = "ACC${i + 1}"
            val ok = ps.filter { !it.fact.failed && it.fact.items.isNotEmpty() }
            if (ok.isEmpty()) return@mapIndexedNotNull if (ps.any { it.fact.failed }) failedSection(id, broker, personal = true) else null
            val evalKrw = ok.mapNotNull { toKrw(it.fact.eval, it) }.sum()
            val profitKrw = ok.flatMap { pt -> pt.fact.items.mapNotNull { h -> h.evalProfit?.let { toKrw(it, pt) } } }.takeIf { it.isNotEmpty() }?.sum()
            val dayKrw = ok.mapNotNull { pt -> pt.fact.dayChange?.let { toKrw(it, pt) } }.takeIf { it.isNotEmpty() }?.sum()
            val stats = listOfNotNull(
                withRate(profitKrw, profitKrw?.let { pr -> (evalKrw - pr).takeIf { it > 0 }?.let { pr / it * 100 } })?.let { AccountStat("총 수익", it) },
                withRate(dayKrw, dayKrw?.let { d -> (evalKrw - d).takeIf { it > 0 }?.let { d / it * 100 } })?.let { AccountStat("일간 수익", it) },
                realizedBy[broker]?.let { AccountStat("실현 (이달)", coloredWon(it)) },
            )
            val cards = ps.filter { it.fact.failed || it.fact.items.isNotEmpty() }.map { pt ->
                if (pt.fact.failed) AccountBroker(name = pt.label, total = null, failed = true)
                else AccountBroker(
                    name = pt.label,
                    total = if (pt.krw) won(Math.round(pt.fact.eval)) else usd(pt.fact.eval),
                    table = stockTable(pt.fact, pt.krw, pt.nameHeader),
                )
            }
            okSection(id, broker, "", personal = true, account = AccountBlock(total = won(Math.round(evalKrw)), stats = stats, brokers = cards))
        }
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
