package com.example.investfeed.domain.holding.service

import com.example.investfeed.common.util.failure
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.dto.req.StockTradeMarket
import com.example.investfeed.domain.holding.dto.req.StockTradeReq
import com.example.investfeed.domain.holding.dto.res.StockTradeHolding
import com.example.investfeed.domain.holding.dto.res.StockTradeItem
import com.example.investfeed.domain.holding.dto.res.StockTradeRes
import com.example.investfeed.domain.holding.dto.res.StockTradeSide
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.kiwoom.holding.client.HoldingClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.order.client.KiwoomTradeHistoryClient
import com.example.investfeed.kiwoom.order.dto.req.KiwoomTradeHistoryReq
import com.example.investfeed.toss.account.client.TossAccountClient
import com.example.investfeed.toss.holding.TossSymbolMapper
import com.example.investfeed.toss.holding.client.TossHoldingClient
import com.example.investfeed.toss.order.client.TossOrderClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

/**
 * 한 종목의 내 매매 (차트의 매수·매도 표시와 평단가 선).
 * 연동한 증권사마다 체결과 보유를 받아 한 목록으로 모은다. 한 증권사의 조회가 실패하면 그 증권사만 빼고 돌려주며,
 * 어느 증권사가 빠졌는지 failedBrokers 로 알린다.
 */
@Service
class StockTradeService(
    private val kiwoomTradeHistoryClient: KiwoomTradeHistoryClient,
    private val holdingClient: HoldingClient,
    private val tossOrderClient: TossOrderClient,
    private val tossHoldingClient: TossHoldingClient,
    private val tossAccountClient: TossAccountClient,
    private val brokerRepository: BrokerRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val KIWOOM = "키움증권"
        private const val TOSS = "토스증권"
        private const val LOOKBACK_MONTHS = 12L         // 오늘부터 이만큼 전까지의 체결을 받는다
        // 키움 거래내역을 한 번에 조회하는 기간. 두 달은 받아 주는 것을 확인했고(2026-11-18), 더 긴 기간은 확인하지 않았다
        private const val KIWOOM_WINDOW_MONTHS = 2L
        private val CODE_PATTERN = Regex("^[A-Za-z0-9.\\-]{1,12}$")
        private val DATE = DateTimeFormatter.BASIC_ISO_DATE
        private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        private val KR_ZONE = ZoneId.of("Asia/Seoul")
        private val US_ZONE = ZoneId.of("America/New_York")
    }

    /** 한 증권사에서 받은 것 */
    private data class BrokerTrades(val trades: List<StockTradeItem>, val holding: StockTradeHolding?)

    fun getTrades(rawStkCd: String, req: StockTradeReq): StockTradeRes {
        // 화면의 국내 종목 코드는 거래소 구분이 붙어 온다(122630_AL). 증권사 조회와 보유 대조는 6자리 코드로 한다.
        val stkCd = rawStkCd.substringBefore('_')
        require(CODE_PATTERN.matches(stkCd)) { "종목 코드를 확인해주세요." }
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
            ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val loginId = userDetails.username
        val today = LocalDate.now(KR_ZONE)
        val from = today.minusMonths(LOOKBACK_MONTHS).plusDays(1)
        val domestic = req.market == StockTradeMarket.KR

        // 연동한 증권사만 조회한다. 값이 null 이면 그 증권사의 조회가 실패한 것
        val results: Map<String, BrokerTrades?> = runBlocking {
            coroutineScope {
                val kiwoom = if (domestic && hasApiKey(loginId, KIWOOM)) {
                    async { fetchOrNull(KIWOOM) { kiwoomTrades(stkCd, from, today) } }
                } else null
                val toss = if (hasApiKey(loginId, TOSS)) {
                    async { fetchOrNull(TOSS) { tossTrades(stkCd, req.market, from, today) } }
                } else null
                listOfNotNull(kiwoom?.let { KIWOOM to it.await() }, toss?.let { TOSS to it.await() }).toMap()
            }
        }
        val fetched = results.values.filterNotNull()

        return StockTradeRes(
            trades = fetched.flatMap { it.trades }.sortedBy { it.dt },
            holdings = fetched.mapNotNull { it.holding } +
                (if (domestic) manualHoldingsOf(userDetails.member.id, stkCd) else emptyList()),
            failedBrokers = results.filterValues { it == null }.keys.toList(),
        )
    }

    private fun hasApiKey(loginId: String, brokerName: String): Boolean {
        val broker = brokerRepository.findByName(brokerName) ?: return false
        return memberApiKeyRepository.existsByMemberLoginIdAndBrokerId(loginId, broker.id)
    }

    /** 한 증권사의 조회가 실패하면 그 증권사만 빼고(null) 나머지 표시는 그대로 보여 준다. */
    private suspend fun fetchOrNull(broker: String, fetch: suspend () -> BrokerTrades): BrokerTrades? = try {
        fetch()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.failure(e) { "내 매매 조회 실패: broker=$broker" }
        null
    }

    /** 키움은 체결일만 준다(체결 시각 없음). 한 주문이 여러 줄로 오므로 화면이 하루 단위로 합친다. */
    private suspend fun kiwoomTrades(stkCd: String, from: LocalDate, to: LocalDate): BrokerTrades {
        val windows = generateSequence(from) { it.plusMonths(KIWOOM_WINDOW_MONTHS) }
            .takeWhile { !it.isAfter(to) }
            .map { start -> start to minOf(to, start.plusMonths(KIWOOM_WINDOW_MONTHS).minusDays(1)) }
            .toList()

        val trades = windows.flatMap { (start, end) ->
            kiwoomTradeHistoryClient.tradeHistory(
                KiwoomTradeHistoryReq(strt_dt = start.format(DATE), end_dt = end.format(DATE), stk_cd = stkCd)
            )
        }.mapNotNull { item ->
            // 종목 지정으로 조회했지만 다른 종목이 섞여 와도 그리지 않게 한 번 더 가린다 (응답의 코드는 A122630 모양)
            if (item.stk_cd?.takeLast(stkCd.length) != stkCd) return@mapNotNull null
            val side = when (item.io_tp) {
                "1" -> StockTradeSide.SELL
                "2" -> StockTradeSide.BUY
                else -> return@mapNotNull null
            }
            val date = item.cntr_dt?.takeIf { it.length == 8 } ?: return@mapNotNull null
            val quantity = positive(item.trde_qty_jwa_cnt) ?: return@mapNotNull null
            val price = positive(item.trde_unit) ?: return@mapNotNull null
            StockTradeItem(broker = KIWOOM, dt = date, side = side, quantity = quantity, price = price)
        }

        // 보유 수량과 평단가는 보유 조회(kt00018)에서 읽는다. member_holdings 에는 연동 계좌의 수량·평단가가 없다.
        val holding = holdingClient.holdingList(KiwoomHoldingReq(qry_tp = "1", dmst_stex_tp = "KRX"))
            .acnt_evlt_remn_indv_tot.orEmpty()
            .firstOrNull { it.stk_cd?.takeLast(stkCd.length) == stkCd }
            ?.let { holdingOf(KIWOOM, it.rmnd_qty, it.pur_pric) }

        return BrokerTrades(trades, holding)
    }

    /** 토스는 국내·미국을 한 조회로 주고 최종 체결 시각을 준다. 취소·거부된 주문(체결 수량 0)은 뺀다. */
    private suspend fun tossTrades(stkCd: String, market: StockTradeMarket, from: LocalDate, to: LocalDate): BrokerTrades {
        val accounts = tossAccountClient.getAccounts()
        val accountSeq = (accounts.firstOrNull { it.accountType == "BROKERAGE" } ?: accounts.firstOrNull())?.accountSeq
            ?: return BrokerTrades(emptyList(), null)
        val us = market == StockTradeMarket.US
        val currency = if (us) "USD" else "KRW"
        val zone = if (us) US_ZONE else KR_ZONE

        val trades = tossOrderClient.closedOrders(accountSeq, stkCd, from, to).mapNotNull { order ->
            if (order.symbol != stkCd || order.currency != currency) return@mapNotNull null
            val side = when (order.side) {
                "BUY" -> StockTradeSide.BUY
                "SELL" -> StockTradeSide.SELL
                else -> return@mapNotNull null
            }
            val execution = order.execution ?: return@mapNotNull null
            val quantity = positive(execution.filledQuantity) ?: return@mapNotNull null
            val price = positive(execution.averageFilledPrice) ?: return@mapNotNull null
            val filledAt = execution.filledAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: return@mapNotNull null
            StockTradeItem(
                broker = TOSS,
                dt = filledAt.atZoneSameInstant(zone).format(DATE_TIME),
                side = side,
                quantity = quantity,
                price = price,
            )
        }

        // 평단가는 종목 통화 기준(국내 원, 미국 달러)이라 차트의 가격과 같은 단위다.
        val holding = tossHoldingClient.getHoldings(accountSeq)?.items.orEmpty()
            .firstOrNull { it.symbol == stkCd && TossSymbolMapper.isUs(it.marketCountry) == us }
            ?.let { holdingOf(TOSS, it.quantity, it.averagePurchasePrice) }

        return BrokerTrades(trades, holding)
    }

    /** 숫자 글자를 읽는다. 쉼표가 들어 있거나 앞을 0 으로 채운 값도 읽고, 0 이하이거나 숫자가 아니면 null */
    private fun positive(raw: String?): Double? = raw?.replace(",", "")?.trim()?.toDoubleOrNull()?.takeIf { it > 0 }

    private fun holdingOf(broker: String, quantity: String?, avgPrice: String?): StockTradeHolding? {
        return StockTradeHolding(broker = broker, quantity = positive(quantity) ?: return null, avgPrice = positive(avgPrice) ?: return null)
    }

    /**
     * 수동 입력 계좌의 보유. 연동 계좌는 증권사 응답에서 읽으므로 여기서는 뺀다(member_holdings 에 수량·평단가가 없다).
     * 평단가가 원화라 국내 종목에만 쓴다. 저장된 종목 코드에는 거래소 구분이 붙어 있을 수 있다(122630_AL).
     */
    private fun manualHoldingsOf(memberId: Long, stkCd: String): List<StockTradeHolding> {
        val manualBrokers = brokerRepository.findAllByType(BrokerType.MANUAL).associate { it.id to it.name }
        return memberHoldingRepository.findByMemberId(memberId).mapNotNull { holding ->
            val broker = manualBrokers[holding.broker.id] ?: return@mapNotNull null
            if (holding.stkCd.endsWith("_US") || holding.stkCd.substringBefore('_').takeLast(stkCd.length) != stkCd) return@mapNotNull null
            val quantity = holding.quantity?.takeIf { it > 0 } ?: return@mapNotNull null
            val avgPrice = holding.purPrice?.takeIf { it > 0 } ?: return@mapNotNull null
            StockTradeHolding(broker = broker, quantity = quantity.toDouble(), avgPrice = avgPrice.toDouble())
        }
    }
}
