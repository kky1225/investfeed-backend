package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.req.SectStockListReq
import com.example.investfeed.domain.sect.service.SectService
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockListReq
import com.example.investfeed.domain.us.sect.service.UsSectService
import com.example.investfeed.internal.assistant.tool.RankToolService.RankMarket
import org.springframework.stereotype.Service
import kotlin.math.abs

/**
 * 업종 당일 등락 (업종 화면과 같은 목록). 국내는 키움 산업 업종(SectService 가 지수·규모·제조 제외), 미국은 키움 미국 업종 41개.
 * sector 가 없으면 카드는 히트맵, 있으면 그 업종의 시장별 소속 종목 (업종 종목 화면과 같은 범위).
 * sort: 업종·종목 정렬. 질문이 "많이 내린"이면 FALL, "거래 많은"이면 VALUE — 카드 첫 화면이 곧 답이라 서버가 정렬한다.
 * 기간 수익률은 비서에서 다루지 않는다 — multiDay 질문이면 안내 문장 + 당일 값 (2026-10-24)
 */
@Service
class SectorToolService(
    private val sectService: SectService,
    private val usSectService: UsSectService,
    private val rankTool: RankToolService,
) {
    /** KR: 코스피·코스닥 둘 다 ("국내 업종"처럼 시장을 특정하지 않은 질문) */
    enum class SectorMarket(val rank: RankMarket) { KR(RankMarket.KR), KOSPI(RankMarket.KR), KOSDAQ(RankMarket.KR), US(RankMarket.US) }

    /** VALUE: 업종은 거래대금, 소속 종목은 거래량 순 (ka20002 에 종목 거래대금이 없다). 미국은 거래대금이 없어 RISE 로 대신 */
    enum class SectorSort { RISE, FALL, VALUE }

    companion object {
        private const val KOSPI = "001"
        private const val KOSDAQ = "101"

        // 키움 산업 업종에 없는 통칭 → 그 종목들이 속한 업종 (대표 종목 소속을 ka20002 로 확인, 2026-10-24)
        // 반도체(SK하이닉스)·디스플레이(LG디스플레이)·2차전지(LG에너지솔루션) → 전기/전자, 자동차(현대차)·조선(HD현대중공업) → 운송장비/부품,
        // 바이오(삼성바이오로직스·HLB) → 제약, 은행(기업은행) → 금융, 철강(POSCO홀딩스) → 금속
        private val KR_ALIAS = mapOf(
            "반도체" to "전기/전자", "디스플레이" to "전기/전자", "2차전지" to "전기/전자", "이차전지" to "전기/전자", "배터리" to "전기/전자",
            "자동차" to "운송장비/부품", "조선" to "운송장비/부품", "바이오" to "제약", "은행" to "금융", "철강" to "금속",
        )
        private val NOISE = Regex("[\\s/·,()]|및|업종|섹터")
        private const val DAILY_ONLY = "업종은 당일(장 시작 전·휴장일에는 직전 거래일) 등락률만 제공합니다."
    }

    data class SectorRow(
        val code: String, val name: String, val link: String, val changeRate: Double?,
        val tradeAmount: Long?,   // 백만원 (국내만)
        val rising: Int?, val falling: Int?,   // 국내만
    )

    /** label: "코스피"·"코스닥"·"미국". 업종 상세면 sectors 는 그 업종 한 행, stocks 는 그 시장 소속 종목 전체(정렬됨) */
    data class SectorGroup(val label: String, val sectors: List<SectorRow>, val stocks: List<SectorStock>? = null)

    data class SectorStock(val code: String, val name: String, val link: String, val changeRate: Double?, val price: Double?, val volume: Long?)

    /** sectorName 이 있으면 업종 상세 (groups 는 그 업종이 있는 시장만, 국내 화학처럼 코스피·코스닥 양쪽이면 2개) */
    data class SectorPerformance(
        val market: RankMarket, val basis: String, val currency: String, val sort: SectorSort,
        val groups: List<SectorGroup>,
        val sectorName: String? = null,
        val notice: String? = null,
    )

    fun sectorPerformance(market: SectorMarket, sector: String?, multiDay: Boolean?, sort: SectorSort?, requested: AssistantDate?): Any {
        val live = rankTool.liveBasis(market.rank, "업종 등락률은", requested)
        live.unavailable?.let { return DataToolService.TextReply(it) }
        val dailyNotice = when {
            multiDay != true -> null
            market == SectorMarket.US -> "$DAILY_ONLY 미국 업종 기간 수익률은 업종 화면에서 볼 수 있습니다."
            else -> DAILY_ONLY
        }
        return if (market == SectorMarket.US) us(sector, sort ?: SectorSort.RISE, live.basis, dailyNotice)
        else kr(market, sector, sort ?: SectorSort.RISE, live.basis, dailyNotice)
    }

    private fun List<SectorRow>.sortRows(sort: SectorSort): List<SectorRow> = when (sort) {
        SectorSort.RISE -> sortedByDescending { it.changeRate ?: Double.NEGATIVE_INFINITY }
        SectorSort.FALL -> sortedBy { it.changeRate ?: Double.POSITIVE_INFINITY }
        SectorSort.VALUE -> sortedByDescending { it.tradeAmount ?: Long.MIN_VALUE }
    }

    private fun List<SectorStock>.sortStocks(sort: SectorSort): List<SectorStock> = when (sort) {
        SectorSort.RISE -> sortedByDescending { it.changeRate ?: Double.NEGATIVE_INFINITY }
        SectorSort.FALL -> sortedBy { it.changeRate ?: Double.POSITIVE_INFINITY }
        SectorSort.VALUE -> sortedByDescending { it.volume ?: Long.MIN_VALUE }
    }

    private fun notices(vararg texts: String?): String? = listOfNotNull(*texts).joinToString(" ").ifEmpty { null }

    // ── 국내 ──

    private fun kr(market: SectorMarket, sector: String?, sort: SectorSort, basis: String, dailyNotice: String?): Any {
        // 시장은 인자 우선, 없으면 "코스닥 화학"처럼 업종명에 붙인 시장
        val only = when {
            market == SectorMarket.KOSPI -> "코스피"
            market == SectorMarket.KOSDAQ -> "코스닥"
            sector?.contains("코스피") == true -> "코스피"
            sector?.contains("코스닥") == true -> "코스닥"
            else -> null
        }
        val groups = listOf("코스피" to KOSPI, "코스닥" to KOSDAQ).filter { only == null || it.first == only }.map { (label, indsCd) ->
            SectorGroup(label, sectService.listSects(SectListReq(indsCd = indsCd)).sectList.orEmpty().mapNotNull { s ->
                val code = s.stkCd ?: return@mapNotNull null
                SectorRow(
                    code = code, name = s.stkNm ?: code, link = "/stock/sect/$code/list",
                    changeRate = TemplateFormat.parse(s.fluRt), tradeAmount = TemplateFormat.parseLong(s.trdePrica),
                    rising = s.rising?.toIntOrNull(), falling = s.fall?.toIntOrNull(),
                )
            }.sortRows(sort))
        }
        if (groups.all { it.sectors.isEmpty() }) throw ToolException("국내 업종 조회에 실패했습니다")
        if (sector.isNullOrBlank()) {
            return SectorPerformance(RankMarket.KR, basis, "KRW", sort, groups, notice = dailyNotice)
        }

        val query = sector.replace("코스피", "").replace("코스닥", "")
        val names = groups.flatMap { g -> g.sectors.map { it.name } }.distinct()
        val (name, aliasNotice) = when (val m = match(query, names)) {
            is Match.Found -> m.name to null
            is Match.Ambiguous -> return DataToolService.TextReply("어느 업종인가요? ${m.names.joinToString(", ")}")
            Match.None -> KR_ALIAS.entries.firstOrNull { norm(query).contains(norm(it.key)) }
                ?.takeIf { it.value in names }
                ?.let { it.value to "키움 업종에는 '${it.key}' 업종이 없어 ${it.key} 종목이 속한 '${it.value}' 업종으로 보여드립니다." }
                ?: return DataToolService.TextReply("'${sector.trim()}' 업종을 찾지 못했습니다. 국내 업종: ${names.joinToString(", ")}")
        }

        // 시장별로 나눈다 — 코스피 화학과 코스닥 화학은 다른 업종 지수라 등락률도 다르다
        val matched = groups.mapNotNull { g ->
            val row = g.sectors.firstOrNull { it.name == name } ?: return@mapNotNull null
            val stocks = sectService.listStocksBySect(row.code, SectStockListReq(mrktTp = if (g.label == "코스닥") "1" else "0"))
                .sectStockList.orEmpty().mapNotNull { s ->
                    val code = s.stkCd?.substringBefore("_") ?: return@mapNotNull null
                    SectorStock(
                        code = code, name = s.stkNm ?: code, link = "/stock/detail/$code", changeRate = TemplateFormat.parse(s.fluRt),
                        price = TemplateFormat.parse(s.curPrc)?.let { abs(it) }, volume = TemplateFormat.parseLong(s.nowTrdeQty),
                    )
                }
            SectorGroup(g.label, listOf(row), stocks.sortStocks(sort))
        }
        return SectorPerformance(RankMarket.KR, basis, "KRW", sort, matched, sectorName = name, notice = notices(dailyNotice, aliasNotice))
    }

    // ── 미국 ──

    private fun us(sector: String?, requestedSort: SectorSort, basis: String, dailyNotice: String?): Any {
        val valueNotice = "미국 업종은 거래대금 정보가 없어 등락률 순으로 보여드립니다.".takeIf { requestedSort == SectorSort.VALUE }
        val sort = if (requestedSort == SectorSort.VALUE) SectorSort.RISE else requestedSort
        val rows = usSectService.listUsSects().sectList.mapNotNull { s ->
            val code = s.indsCd ?: return@mapNotNull null
            SectorRow(
                code = code, name = s.indsNm ?: code, link = "/us-stock/sect/$code/list", changeRate = TemplateFormat.parse(s.perf1d),
                tradeAmount = null, rising = null, falling = null,
            )
        }.sortRows(sort)
        if (rows.isEmpty()) throw ToolException("미국 업종 조회에 실패했습니다")
        if (sector.isNullOrBlank()) {
            return SectorPerformance(RankMarket.US, basis, "USD", sort, listOf(SectorGroup("미국", rows)), notice = notices(dailyNotice, valueNotice))
        }

        val names = rows.map { it.name }
        val name = when (val m = match(sector, names)) {
            is Match.Found -> m.name
            is Match.Ambiguous -> return DataToolService.TextReply("어느 업종인가요? ${m.names.joinToString(", ")}")
            Match.None -> return DataToolService.TextReply("'${sector.trim()}' 업종을 찾지 못했습니다. 미국 업종: ${names.joinToString(", ")}")
        }
        val row = rows.first { it.name == name }
        val stocks = usSectService.listStocksBySect(row.code, UsSectStockListReq(sortTp = if (sort == SectorSort.FALL) "2" else "1"))
            .sectStockList.mapNotNull { s ->
                val code = s.stkCd ?: return@mapNotNull null
                SectorStock(
                    code = code, name = s.stkNm ?: code, link = "/us-stock/detail/${s.stexTp}/$code", changeRate = TemplateFormat.parse(s.fluRt),
                    price = TemplateFormat.parse(s.curPrc)?.let { abs(it) }, volume = TemplateFormat.parseLong(s.accTrdeQty),
                )
            }
        return SectorPerformance(
            RankMarket.US, basis, "USD", sort, listOf(SectorGroup("미국", listOf(row), stocks.sortStocks(sort))),
            sectorName = name, notice = notices(dailyNotice, valueNotice),
        )
    }

    // ── 업종명 해석 ──

    private sealed interface Match {
        data class Found(val name: String) : Match
        data class Ambiguous(val names: List<String>) : Match
        data object None : Match
    }

    private fun norm(s: String) = s.replace(NOISE, "").lowercase()

    /** 정확히 같은 이름 우선, 없으면 부분 일치(양방향). 부분 일치가 여럿이면 되묻는다 */
    private fun match(query: String, names: List<String>): Match {
        val q = norm(query)
        if (q.isEmpty()) return Match.None
        names.firstOrNull { norm(it) == q }?.let { return Match.Found(it) }
        val partial = names.filter { norm(it).contains(q) || q.contains(norm(it)) }
        return when (partial.size) {
            0 -> Match.None
            1 -> Match.Found(partial.first())
            else -> Match.Ambiguous(partial)
        }
    }
}
