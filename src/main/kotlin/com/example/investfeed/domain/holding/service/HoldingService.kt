package com.example.investfeed.domain.holding.service

import com.example.investfeed.kiwoom.exception.UsHoldingListException
import com.example.investfeed.kiwoom.exception.UsDepositException
import com.example.investfeed.kiwoom.exception.HoldingListException
import com.example.investfeed.kiwoom.exception.DepositException
import com.example.investfeed.common.util.MarketTimeUtil.isKrxHoldingClose
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.domain.us.stock.service.UsEtfService
import com.example.investfeed.kiwoom.holding.client.HoldingClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomDepositReq
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingReq
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomUsStreamItem
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.us.holding.client.UsHoldingClient
import com.example.investfeed.kiwoom.us.holding.dto.req.KiwoomUsDepositReq
import com.example.investfeed.kiwoom.us.holding.dto.req.KiwoomUsHoldingReq
import com.example.investfeed.kiwoom.us.holding.dto.res.KiwoomUsHoldingRes
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

@Service
class HoldingService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val holdingClient: HoldingClient,
    private val usHoldingClient: UsHoldingClient,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val usEtfService: UsEtfService,
    private val stockClient: StockClient,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val US_SUFFIX = "_US"
        private const val CURRENCY_USD = "USD"
    }

    fun listHoldings(): HoldingListRes = runBlocking { listHoldingsSuspend() }

    suspend fun listHoldingsSuspend(): HoldingListRes = coroutineScope {
        val holdingDeferred = async {
            holdingClient.holdingList(
                req = KiwoomHoldingReq(
                    qry_tp = "1",
                    dmst_stex_tp = if(isKrxHoldingClose()) "NXT" else "KRX"
                )
            )
        }
        val depositDeferred = async { holdingClient.deposit(KiwoomDepositReq(qry_tp = "3")) }
        val usHoldingDeferred = async { usHoldingClient.usHoldingList(KiwoomUsHoldingReq()) }
        val usdDepositDeferred = async { fetchUsdDeposit() }
        val basePricDeferred = async {
            fetchBasePrics(holdingDeferred.await().acnt_evlt_remn_indv_tot?.mapNotNull { it.stk_cd } ?: emptyList())
        }

        val res = holdingDeferred.await()
        val rows = res.acnt_evlt_remn_indv_tot ?: emptyList()
        val basePricByStkCd = basePricDeferred.await()

        val holdingList = rows.map { stock ->
            val rawCd = stock.stk_cd?.removePrefix("A") ?: ""
            HoldingItem(
                stkCd = rawCd + "_AL",
                stkNm = stock.stk_nm ?: "",
                curPrc = stock.cur_prc?.replace("^[+-]".toRegex(), "") ?: "0",
                purPric = stock.pur_pric ?: "0",
                purAmt = stock.pur_amt ?: "0",
                evltAmt = stock.evlt_amt ?: "0",
                evltvPrft = stock.evltv_prft ?: "0",
                prftRt = stock.prft_rt ?: "0",
                rmndQty = stock.rmnd_qty ?: "0",
                possRt = stock.poss_rt ?: "0",
                predClosePric = basePricByStkCd[rawCd]
                    ?: stock.pred_close_pric?.replace("^[+-]".toRegex(), "") ?: "0",
            )
        }

        val memberId = getMemberId()
        var sortedHoldingList = holdingList
        if (memberId != null && holdingList.isNotEmpty()) {
            val kiwoomBroker = brokerRepository.findByName("키움증권")
            if (kiwoomBroker != null) {
                memberHoldingSyncService.sync(
                    memberId = memberId,
                    holdings = holdingList.map { it.stkCd to it.stkNm },
                    broker = kiwoomBroker
                )

                val memberHoldings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, kiwoomBroker.id)
                val holdingMap = holdingList.associateBy { it.stkCd }
                sortedHoldingList = memberHoldings.mapNotNull { mh ->
                    holdingMap[mh.stkCd]?.copy(id = mh.id)
                }
            }
        }

        val depositRes = depositDeferred.await()

        val balance = depositRes.d2_entra ?: throw DepositException()
        val usRes = usHoldingDeferred.await()
        val usHoldingList = toUsHoldingItems(usRes)
        val usdDeposit = usdDepositDeferred.await()

        val totPurAmt = res.tot_pur_amt.toAmount("tot_pur_amt") { HoldingListException() } +
                usRes.tot_prch_amt_krw.toAmount("tot_prch_amt_krw") { UsHoldingListException() }
        val totEvltAmt = res.tot_evlt_amt.toAmount("tot_evlt_amt") { HoldingListException() } +
                usRes.tot_evlt_amt_krw.toAmount("tot_evlt_amt_krw") { UsHoldingListException() }
        val totEvltPl = res.tot_evlt_pl.toAmount("tot_evlt_pl") { HoldingListException() } +
                usRes.tot_pl_amt_krw.toAmount("tot_pl_amt_krw") { UsHoldingListException() }

        HoldingListRes(
            totPurAmt = totPurAmt.toString(),
            totEvltAmt = totEvltAmt.toString(),
            totEvltPl = totEvltPl.toString(),
            totPrftRt = if (totPurAmt != 0L) String.format("%.2f", totEvltPl.toDouble() / totPurAmt * 100) else "0",
            balance = balance,
            balanceUsd = usdDeposit?.first,
            balanceUsdKrw = usdDeposit?.second,
            holdingList = withPossRt(sortedHoldingList + usHoldingList, totEvltAmt),
        )
    }

    private fun withPossRt(holdings: List<HoldingItem>, totEvltAmt: Long): List<HoldingItem> {
        if (holdings.none { it.stkCd.endsWith(US_SUFFIX) } || totEvltAmt <= 0L) return holdings

        return holdings.map {
            it.copy(possRt = String.format("%.2f", it.evltAmt.toAmount("evltAmt") { HoldingListException() }.toDouble() / totEvltAmt * 100))
        }
    }

    private suspend fun fetchUsdDeposit(): Pair<String, String>? {
        return try {
            val usd = usHoldingClient.usDeposit(KiwoomUsDepositReq())
                .result_list
                ?.firstOrNull { it.crnc_code == CURRENCY_USD }
            (usd?.fc_entra ?: "0") to (usd?.let { it.fc_booka.toAmount("fc_booka") { UsDepositException() } } ?: 0L).toString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "해외 외화예수금 조회 실패 — 달러 표시를 생략합니다: ${e.message}" }
            null
        }
    }

    private fun toUsHoldingItems(usRes: KiwoomUsHoldingRes?): List<HoldingItem> {
        val rows = usRes?.result_list?.filter { !it.stk_cd.isNullOrBlank() } ?: return emptyList()
        if (rows.isEmpty()) return emptyList()

        val tickers = rows.mapNotNull { it.stk_cd }.distinct()
        val stexTpByTicker = usStockMasterRepository.findByStkCdIn(tickers).associate { it.stkCd to it.stexTp }
        val etfTickers = usEtfService.etfTickers(tickers)

        val missing = tickers - stexTpByTicker.keys
        if (missing.isNotEmpty()) {
            log.warn { "미국 종목 마스터 미등록으로 거래소구분을 채우지 못했습니다: $missing" }
        }

        return rows.map { row ->
            val ticker = row.stk_cd!!
            HoldingItem(
                stkCd = "${ticker}_US",
                stkNm = usEtfService.displayName(ticker, row.frgn_stk_nm, etfTickers) ?: ticker,
                curPrc = row.now_pric_krw.toAmount("now_pric_krw") { UsHoldingListException() }.toString(),
                purPric = row.frgn_stk_book_uv_krw.toAmount("frgn_stk_book_uv_krw") { UsHoldingListException() }.toString(),
                purAmt = row.frgn_stk_book_amt_krw.toAmount("frgn_stk_book_amt_krw") { UsHoldingListException() }.toString(),
                evltAmt = row.evlt_amt_krw.toAmount("evlt_amt_krw") { UsHoldingListException() }.toString(),
                evltvPrft = row.pl_amt_krw.toAmount("pl_amt_krw") { UsHoldingListException() }.toString(),
                prftRt = row.pl_rt ?: "0",
                rmndQty = row.poss_qty.toAmount("poss_qty") { UsHoldingListException() }.toString(),
                possRt = "0",
                predClosePric = "0",
                stexTp = stexTpByTicker[ticker],
                usStkCd = ticker,
                curPrcUsd = row.now_pric,
                purPricUsd = row.frgn_stk_book_uv,
                evltAmtUsd = row.evlt_amt,
                evltvPrftUsd = row.pl_amt,
            )
        }
    }

    private suspend fun fetchBasePrics(stkCds: List<String>): Map<String, String> {
        val codes = stkCds.map { it.removePrefix("A") }.filter { it.isNotBlank() }.distinct()
        if (codes.isEmpty()) return emptyMap()

        return try {
            stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = codes.joinToString("|")))
                .atn_stk_infr
                ?.mapNotNull { info ->
                    val cd = info.stk_cd?.removePrefix("A") ?: return@mapNotNull null
                    val base = info.base_pric?.replace("^[+-]".toRegex(), "")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    cd to base
                }
                ?.toMap()
                ?: emptyMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn { "보유종목 기준가 조회 실패 — 일간수익이 부정확할 수 있습니다: ${e.message}" }
            emptyMap()
        }
    }

    private fun String?.toAmount(field: String, onError: () -> RuntimeException): Long {
        return this?.trim()?.toLongOrNull() ?: run {
            log.error { "키움 금액 필드 파싱 실패 : $field=$this" }
            throw onError()
        }
    }

    fun streamHoldings(req: HoldingStreamReq) {
        val (usCodes, krCodes) = req.items.partition { it.endsWith(US_SUFFIX) }

        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.NXT,
                items = listOf(""), // 주문체결은 계좌 단위라 종목코드가 필요 없음
                types = listOf("04")
            ),
            StreamEntry(
                market = StreamMarket.NXT,
                items = krCodes,
                types = listOf("0B")
            ),
            StreamEntry(
                market = StreamMarket.US,
                items = usStreamItems(usCodes),
                types = listOf("FE")
            )
        )
    }

    private fun usStreamItems(usCodes: List<String>): List<KiwoomUsStreamItem> {
        if (usCodes.isEmpty()) return emptyList()

        val tickers = usCodes.map { it.removeSuffix(US_SUFFIX) }.distinct()
        val stexTpByTicker = usStockMasterRepository.findByStkCdIn(tickers).associate { it.stkCd to it.stexTp }

        val items = tickers.mapNotNull { ticker ->
            val stexTp = stexTpByTicker[ticker] ?: return@mapNotNull null
            KiwoomUsStreamItem(jmcode = ticker, stex_tp = stexTp)
        }

        if (items.isEmpty()) {
            log.warn { "미국 보유종목 실시간 등록 실패 — 마스터에 거래소구분이 없습니다: $tickers" }
        }

        return items
    }

    private fun getMemberId(): Long? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id
    }
}
