package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.assistant.dto.factsheet.HoldingAlertHit
import com.example.investfeed.domain.assistant.service.UsMarketCalendarService
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class UsHoldingAlertChecker(
    private val usStockClient: UsStockClient,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val usMarketCalendarService: UsMarketCalendarService,
    private val usSessionBaseService: UsSessionBaseService,
    private val brokerRepository: BrokerRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val judge: PriceAlertJudge,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        val US_STOCK_THRESHOLDS = listOf(5.0, 10.0, 15.0, 20.0, 30.0)
        const val US_SUFFIX = "_US"
        private const val TOSS_BROKER_NAME = "토스증권"
        private val PRE_OPEN_ET: LocalTime = LocalTime.of(4, 0)
        private val US_OPEN_ET: LocalTime = LocalTime.of(9, 30)
        private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    fun check(usHoldings: List<MemberHolding>, hits: MutableList<HoldingAlertHit>, now: LocalDateTime) {
        if (usHoldings.isEmpty()) return

        val ny = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val usDate = ny.toLocalDate()
        val nyTime = ny.toLocalTime()
        val regular = usMarketCalendarService.isUsTradingDay(usDate) && !nyTime.isBefore(US_OPEN_ET) && !nyTime.isAfter(usMarketCalendarService.closeTimeEt(usDate))
        if (regular) checkRegular(usHoldings, hits, usDate) else checkExtSession(usHoldings, hits, now)
    }

    /** 정규장: 전일 종가(키움 base_close_pric) 대비 당일 고가·저가 */
    private fun checkRegular(usHoldings: List<MemberHolding>, hits: MutableList<HoldingAlertHit>, usDate: LocalDate) {
        val byTicker = usHoldings.groupBy { it.stkCd.removeSuffix(US_SUFFIX) }
        val stexByTicker = stexByTicker(byTicker.keys)
        val todayUs = usDate.format(YYYYMMDD)
        val seen = mutableSetOf<Pair<Long, String>>()

        byTicker.forEach { (ticker, holders) ->
            val stexTp = stexByTicker[ticker] ?: run { log.warn { "미국 보유 종목 거래소 구분 없음(us_stock_master): $ticker" }; return@forEach }
            val res = stockInfo(stexTp, ticker) ?: return@forEach
            val base = price(res.base_close_pric) ?: return@forEach
            val high = price(res.high_pric) ?: return@forEach
            val low = price(res.low_pric) ?: return@forEach
            val curPrice = price(res.cur_prc)
            val curRate = curPrice?.let { (it - base) / base * 100 }
            val maxUpRt = (high - base) / base * 100
            val maxDownRt = (low - base) / base * 100
            val link = "/us-stock/detail/$stexTp/$ticker"

            holders.filter { seen.add(it.memberId to it.stkCd) }.forEach { h ->
                val t = AlertTarget(h.memberId, AssetType.STOCK, h.stkCd, h.stkNm, link, held = true, curRate = curRate, curPrice = curPrice)
                if (maxUpRt > 0) judge.judge(t, Direction.UP, maxUpRt, US_STOCK_THRESHOLDS, hits, usDate)
                if (maxDownRt < 0) judge.judge(t, Direction.DOWN, maxDownRt, US_STOCK_THRESHOLDS, hits, usDate)
                if (res.wk52_hgst_pric_dt == todayUs) judge.judge(t, Direction.HIGH_52W, high, PriceAlertJudge.SINGLE, hits, usDate)
                if (res.wk52_lwst_pric_dt == todayUs) judge.judge(t, Direction.LOW_52W, low, PriceAlertJudge.SINGLE, hits, usDate)
            }
        }
    }

    /**
     * 장외(애프터·데이·프리): 직전 정규장 공식 종가 대비 현재가 (2026-10-29).
     * - 토스 연동 회원: 토스 일봉 종가로 세 구간 모두
     * - 그 외 회원(토스 조회 실패 포함): 프리마켓만 키움 기준가. 키움은 프리마켓 무렵 거래일을 넘겨 base_close_pric 이
     *   직전 정규장 종가가 되고, 애프터·데이마켓에는 그 전날 종가로 남아 있다 (10-29 실측: 데이마켓 101.08 → 프리마켓 107.35)
     * 장외에서 보낸 알림도 다음 정규장에서 다시 보낸다 — 중복 방지는 AlertSession.EXT 로 따로 센다
     */
    private fun checkExtSession(usHoldings: List<MemberHolding>, hits: MutableList<HoldingAlertHit>, now: LocalDateTime) {
        val holdingsByMember = usHoldings.groupBy { it.memberId }
        val tossBases = tossBases(holdingsByMember)
        val kiwoomMembers = if (isPreMarket(now)) holdingsByMember.keys - tossBases.keys else emptySet()
        if (tossBases.isEmpty() && kiwoomMembers.isEmpty()) return   // 정규장·휴장·장 사이 공백

        val tickers = tossBases.values.flatMap { it.closes.keys } +
            kiwoomMembers.flatMap { m -> holdingsByMember.getValue(m).map { it.stkCd.removeSuffix(US_SUFFIX) } }
        val quotes = quotes(tickers.toSet())

        tossBases.forEach { (memberId, base) ->
            holdingsByMember.getValue(memberId).distinctBy { it.stkCd }.forEach { h ->
                val close = base.closes[h.stkCd.removeSuffix(US_SUFFIX)] ?: return@forEach
                val (stexTp, res) = quotes[h.stkCd.removeSuffix(US_SUFFIX)] ?: return@forEach
                judgeExt(h, stexTp, close, price(res.cur_prc) ?: return@forEach, base.baseDate, base.session, hits)
            }
        }

        if (kiwoomMembers.isEmpty()) return
        val baseDate = usMarketCalendarService.lastClosedUsTradingDay(now)
        kiwoomMembers.flatMap { holdingsByMember.getValue(it) }.distinctBy { it.memberId to it.stkCd }.forEach { h ->
            val (stexTp, res) = quotes[h.stkCd.removeSuffix(US_SUFFIX)] ?: return@forEach
            if (price(res.open_pric) != null) return@forEach   // 당일 시가가 남아 있으면 아직 거래일을 안 넘긴 것 → base_close_pric 이 직전 정규장 종가가 아니다
            val close = price(res.base_close_pric) ?: return@forEach
            judgeExt(h, stexTp, close, price(res.cur_prc) ?: return@forEach, baseDate, UsSessionBaseService.ExtSession.PRE, hits)
        }
    }

    private fun judgeExt(
        h: MemberHolding, stexTp: String, close: Double, curPrice: Double,
        baseDate: LocalDate, session: UsSessionBaseService.ExtSession, hits: MutableList<HoldingAlertHit>,
    ) {
        val ticker = h.stkCd.removeSuffix(US_SUFFIX)
        val rate = (curPrice - close) / close * 100
        val direction = when {
            rate > 0 -> Direction.UP
            rate < 0 -> Direction.DOWN
            else -> return
        }
        val t = AlertTarget(h.memberId, AssetType.STOCK, h.stkCd, h.stkNm, "/us-stock/detail/$stexTp/$ticker", held = true, curRate = rate, curPrice = curPrice)
        judge.judge(t, direction, rate, US_STOCK_THRESHOLDS, hits, baseDate, session)
    }

    /** 토스 연동 회원별 기준 종가. 회원 본인 토스 키로 받고, 종가는 Redis 에 종목·날짜별로 남아 같은 종목은 처음 한 번만 토스를 부른다 */
    private fun tossBases(holdingsByMember: Map<Long, List<MemberHolding>>): Map<Long, UsSessionBaseService.SessionBase> {
        val tossBroker = brokerRepository.findByName(TOSS_BROKER_NAME) ?: return emptyMap()
        return memberApiKeyRepository.findAllByBrokerId(tossBroker.id)
            .filter { it.member.id in holdingsByMember }
            .associate { it.member.id to it.member.loginId }
            .mapNotNull { (memberId, loginId) ->
                val tickers = holdingsByMember.getValue(memberId).map { it.stkCd }
                val base = try {
                    withMemberContext(loginId) { runBlocking { usSessionBaseService.currentBases(tickers) } }
                } catch (e: Exception) {
                    log.error(e) { "미국 장외 기준 종가 조회 실패: member=$memberId" }
                    null
                }
                base?.let { memberId to it }
            }.toMap()
    }

    /** 프리마켓 ET 04:00~09:30 (미국 거래일) */
    private fun isPreMarket(now: LocalDateTime): Boolean {
        val ny = now.atZone(UsMarketCalendarService.KST).withZoneSameInstant(UsMarketCalendarService.NY)
        val t = ny.toLocalTime()
        return usMarketCalendarService.isUsTradingDay(ny.toLocalDate()) && !t.isBefore(PRE_OPEN_ET) && t.isBefore(US_OPEN_ET)
    }

    private fun quotes(tickers: Set<String>): Map<String, Pair<String, KiwoomUsStockInfoRes>> {
        val stexByTicker = stexByTicker(tickers)
        return tickers.mapNotNull { ticker ->
            val stexTp = stexByTicker[ticker] ?: run { log.warn { "미국 보유 종목 거래소 구분 없음(us_stock_master): $ticker" }; return@mapNotNull null }
            stockInfo(stexTp, ticker)?.let { ticker to (stexTp to it) }
        }.toMap()
    }

    private fun stexByTicker(tickers: Collection<String>): Map<String, String> =
        usStockMasterRepository.findByStkCdIn(tickers).groupBy { it.stkCd }.mapValues { (_, rows) -> rows.first().stexTp }

    private fun stockInfo(stexTp: String, ticker: String): KiwoomUsStockInfoRes? = try {
        usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = stexTp, stk_cd = ticker))
    } catch (e: Exception) {
        log.warn { "미국 보유 종목 시세 조회 실패: $ticker ${e.message}" }
        null
    }

    /** 토스는 회원 본인 키로만 부를 수 있어 잠시 그 회원으로 바꿨다가 스케줄러 인증으로 되돌린다 (키움 호출은 스케줄러 인증) */
    private fun <T> withMemberContext(loginId: String, block: () -> T): T {
        val context = SecurityContextHolder.getContext()
        val previous = context.authentication
        context.authentication = UsernamePasswordAuthenticationToken(loginId, null, emptyList())
        try {
            return block()
        } finally {
            context.authentication = previous
        }
    }

    private fun price(s: String?): Double? = s?.toDoubleOrNull()?.let { abs(it) }?.takeIf { it > 0 }
}
