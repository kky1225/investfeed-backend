package com.example.investfeed.internal.assistant.tool

import com.example.investfeed.domain.assistant.service.TemplateFormat
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.domain.rank.dto.req.RankListReq
import com.example.investfeed.domain.rank.service.RankService
import com.example.investfeed.domain.us.rank.dto.req.UsRankListReq
import com.example.investfeed.domain.us.rank.service.UsRankService
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.us.chart.client.UsStockChartClient
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

@Service
class RankToolService(
    private val rankService: RankService,
    private val usRankService: UsRankService,
    private val holidayService: HolidayService,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val usStockChartClient: UsStockChartClient,
    private val cryptoService: CryptoService,
) {
    private val log = KotlinLogging.logger {}

    enum class RankMarket { KR, US, CRYPTO }

    enum class RankType(val code: String, val label: String) {
        VALUE("0", "거래대금"), VOLUME("1", "거래량"), SURGE("2", "거래량 급증률"),
    }

    companion object {
        const val MAX_RANK_STOCKS = 100   // 카드는 처음 10개만 보이고 더보기로 카드 안 스크롤 (투자자별 순위와 같음)
        private val KR_OPEN: LocalTime = LocalTime.of(9, 0)
        private val KR_CLOSE: LocalTime = LocalTime.of(15, 30)
        private val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
        private const val US_SESSION_TICKER = "SPY"
    }

    /** value: 순위 기준 값 (단위는 valueLabel — 순위 화면 열 제목과 같음. 급증률은 %) */
    data class RankStock(
        val rank: Int, val code: String, val name: String, val link: String,
        val changeRate: Double?, val price: Double?, val value: Double?,
    )

    /** basis: "10/17(금) 장중"·"10/17(금) 기준"·"현지 10/16(목) 기준" */
    data class TradingRank(
        val market: RankMarket, val type: RankType, val basis: String, val currency: String, val valueLabel: String,
        val stocks: List<RankStock>,
    )

    private data class Session(val date: LocalDate, val live: Boolean)

    data class LiveBasis(val basis: String, val unavailable: String?)

    fun tradingRank(market: RankMarket, type: RankType, requested: AssistantDate?): Any {
        if (market == RankMarket.CRYPTO) return crypto(type, requested)
        val live = liveBasis(market, "${type.label} 순위는", requested)
        live.unavailable?.let { return DataToolService.TextReply(it) }
        return if (market == RankMarket.KR) kr(type, live.basis) else us(type, live.basis)
    }

    /** subject: 지난 날짜 안내 문장의 주어 ("거래대금 순위는", "업종 등락률은") */
    fun liveBasis(market: RankMarket, subject: String, requested: AssistantDate?): LiveBasis {
        if (market == RankMarket.KR) {
            val session = krSession()
            return LiveBasis(basis(session), requested?.resolve(StockMarket.KR)?.let { unavailableDay(market, subject, it, session) })
        }
        // 미국 "지금" 거래일은 달력이 아니라 최근 체결로 정한다 — 키움 순위는 데이마켓부터 다음 거래일 값이라
        // 달력 기준(현지 날짜)으로 판정하면 끝난 장의 날짜("어제")가 통과해 데이마켓 순위가 나간다 (2026-10-20)
        val tick = usLatestSession()
        val session = tick?.let { Session(it.first, true) } ?: usSession()
        val basis = tick?.let { (date, s) -> "${TemplateFormat.monthDay(date)} ${s.label}" } ?: basis(session, "현지 ")
        return LiveBasis(basis, requested?.resolve(StockMarket.US)?.let { unavailableDay(market, subject, it, session) })
    }

    /** 미국 최근 체결이 속한 거래일·장구분 (SPY 기준 — 데이마켓·프리·정규·애프터 모두 거래가 많다). 실패하면 null → 달력 기준 */
    private fun usLatestSession(): Pair<LocalDate, UsMarketCalendarService.UsSession>? =
        runCatching { usStockChartClient.usStockLatestTick(KiwoomUsStockChartReq(stex_tp = "NY", stk_cd = US_SESSION_TICKER)) }
            .onFailure { log.error(it) { "미국 최근 체결 조회 실패 $US_SESSION_TICKER" } }.getOrNull()
            ?.let { usMarketCalendarService.tickSession(it.cntr_tm, it.bus_dt) }

    private fun krSession(now: LocalDateTime = LocalDateTime.now()): Session {
        val today = now.toLocalDate()
        val open = !holidayService.isHoliday(today) && !now.toLocalTime().isBefore(KR_OPEN)
        return Session(if (open) today else holidayService.lastTradingDay(today.minusDays(1)), open && now.toLocalTime().isBefore(KR_CLOSE))
    }

    private fun usSession(): Session {
        val nowEt = LocalDateTime.now().atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val d = nowEt.toLocalDate()
        val started = usMarketCalendarService.isUsTradingDay(d) && !nowEt.toLocalTime().isBefore(US_OPEN_ET)
        val date = if (started) d else usMarketCalendarService.lastClosedUsTradingDay()
        return Session(date, started && nowEt.toLocalTime().isBefore(usMarketCalendarService.closeTimeEt(d)))
    }

    private fun unavailableDay(market: RankMarket, subject: String, date: LocalDate, session: Session): String? {
        val md = TemplateFormat.monthDay(date)
        val past = "$subject 지난 날짜를 조회할 수 없습니다. 당일(장 시작 전·휴장일에는 직전 거래일) 값만 볼 수 있습니다."
        return when (market) {
            RankMarket.KR -> {
                val today = LocalDate.now()
                when {
                    date.isAfter(today) -> "${md}은 아직 장이 열리지 않았습니다."
                    holidayService.isHoliday(date) -> "${md}은 국내 증시 휴장일입니다."
                    date == today && LocalTime.now().isBefore(KR_OPEN) -> "오늘 국내 장은 아직 열리지 않았습니다. (09:00 개장)"
                    date != session.date -> past
                    else -> null
                }
            }
            RankMarket.US -> when {
                !usMarketCalendarService.isUsTradingDay(date) -> "미국 증시는 현지 ${md} 휴장일입니다."
                date.isAfter(session.date) -> "미국 증시 현지 ${md} 장은 아직 열리지 않았습니다."
                date != session.date -> past
                else -> null
            }
            RankMarket.CRYPTO -> null   // 코인은 tradingRank 에서 따로 처리 (장 구분 없음)
        }
    }

    private fun basis(session: Session, prefix: String = "") =
        "$prefix${TemplateFormat.monthDay(session.date)} ${if (session.live) "장중" else "기준"}"

    private fun kr(type: RankType, basis: String): TradingRank {
        val res = rankService.listRanks(RankListReq(type = type.code))
        if (res.return_code != 0) throw ToolException("국내 ${type.label} 순위 조회에 실패했습니다")
        val stocks = res.rankList.orEmpty().mapIndexedNotNull { i, r ->
            val code = r.stkCd?.substringBefore("_") ?: return@mapIndexedNotNull null
            RankStock(
                rank = r.rank?.toIntOrNull() ?: (i + 1), code = code, name = r.stkNm ?: code, link = "/stock/detail/$code",
                changeRate = TemplateFormat.parse(r.fluRt), price = TemplateFormat.parse(r.curPrc)?.let { abs(it) },
                value = TemplateFormat.parse(r.trdePrica)?.let { abs(it) },
            )
        }.take(MAX_RANK_STOCKS)
        val valueLabel = when (type) { RankType.VALUE -> "거래대금(백만)"; RankType.VOLUME -> "거래량"; RankType.SURGE -> "급증률" }
        return TradingRank(RankMarket.KR, type, basis, "KRW", valueLabel, stocks)
    }

    private fun us(type: RankType, basis: String): TradingRank {
        val res = usRankService.listUsRanks(UsRankListReq(type = type.code))
        if (res.return_code != 0) throw ToolException("미국 ${type.label} 순위 조회에 실패했습니다")
        val stocks = res.rankList.orEmpty().mapIndexedNotNull { i, r ->
            val code = r.stkCd ?: return@mapIndexedNotNull null
            RankStock(
                rank = r.rank?.toIntOrNull() ?: (i + 1), code = code, name = r.stkNm ?: code, link = "/us-stock/detail/${r.stexTp}/$code",
                changeRate = TemplateFormat.parse(r.fluRt), price = TemplateFormat.parse(r.curPrc)?.let { abs(it) },
                value = TemplateFormat.parse(r.trdePrica)?.let { abs(it) },
            )
        }.take(MAX_RANK_STOCKS)
        val valueLabel = when (type) { RankType.VALUE -> "거래대금(천 USD)"; RankType.VOLUME -> "거래량"; RankType.SURGE -> "급증률" }
        return TradingRank(RankMarket.US, type, basis, "USD", valueLabel, stocks)
    }

    /**
     * 24시간 누적값이라 지난 날짜는 조회할 수 없고, 거래량·급증률 순위는 화면에 없어 안내 문장으로 답한다
     */
    private fun crypto(type: RankType, requested: AssistantDate?): Any {
        if (type != RankType.VALUE) return DataToolService.TextReply("코인은 거래대금 순위만 제공합니다.")
        if (requested != null && requested.resolve(StockMarket.CRYPTO) != LocalDate.now()) {
            return DataToolService.TextReply("코인 거래대금 순위는 지난 날짜를 조회할 수 없습니다. 최근 24시간 값만 볼 수 있습니다.")
        }
        val stocks = cryptoService.listCryptoRanks().take(MAX_RANK_STOCKS).mapIndexed { i, c ->
            RankStock(
                rank = i + 1, code = c.market, name = c.koreanName, link = "/crypto/detail/${c.market}",
                changeRate = c.signedChangeRate * 100, price = c.tradePrice, value = c.accTradePrice24h / 1_000_000,
            )
        }
        return TradingRank(RankMarket.CRYPTO, type, "최근 24시간", "KRW", "거래대금(백만)", stocks)
    }
}
