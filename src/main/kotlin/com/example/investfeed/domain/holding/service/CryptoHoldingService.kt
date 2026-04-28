package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.upbit.holding.client.CryptoHoldingClient
import com.example.investfeed.upbit.market.client.MarketClient
import com.example.investfeed.upbit.ticker.client.TickerClient
import com.example.investfeed.upbit.websocket.client.CryptoStreamClient
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CryptoHoldingService(
    private val cryptoHoldingClient: CryptoHoldingClient,
    private val cryptoStreamClient: CryptoStreamClient,
    private val tickerClient: TickerClient,
    private val marketClient: MarketClient,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
) {
    private val log = KotlinLogging.logger {}

    fun listCryptoHoldings(): HoldingListRes {
        val loginId = getLoginId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val memberId = getMemberId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")

        val upbitBroker = brokerRepository.findByName("업비트")
            ?: throw IllegalArgumentException("업비트 거래소를 찾을 수 없습니다.")

        val apiKey = memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, upbitBroker.id)
            ?: throw IllegalArgumentException("업비트 API Key가 등록되지 않았습니다.")

        val accounts = cryptoHoldingClient.getAccounts(apiKey.appKey, apiKey.secretKey)

        // KRW 잔액과 코인 보유 분리
        val krwAccount = accounts.firstOrNull { it.currency == "KRW" }
        val coinAccounts = accounts.filter { it.currency != "KRW" && it.unit_currency == "KRW" }

        if (coinAccounts.isEmpty()) {
            // DB 동기화 (전부 매도된 경우)
            memberHoldingSyncService.sync(memberId, emptyList(), upbitBroker)

            return HoldingListRes(
                totPurAmt = "0",
                totEvltAmt = "0",
                totEvltPl = "0",
                totPrftRt = "0",
                balance = krwAccount?.balance ?: "0",
                holdingList = emptyList()
            )
        }

        // 마켓 코드로 변환하여 현재가 조회
        val marketNames = marketClient.getKrwMarkets().associate { it.market.removePrefix("KRW-") to it }
        val markets = coinAccounts.mapNotNull { account ->
            val market = "KRW-${account.currency}"
            if (marketNames.containsKey(account.currency)) market else null
        }

        val tickerMap = if (markets.isNotEmpty()) {
            tickerClient.getTickers(markets.joinToString(","))
                .associateBy { it.market?.removePrefix("KRW-") ?: "" }
        } else emptyMap()

        var totPurAmt = 0.0
        var totEvltAmt = 0.0

        val holdingList = coinAccounts.mapNotNull { account ->
            val currency = account.currency ?: return@mapNotNull null
            val ticker = tickerMap[currency]
            val marketInfo = marketNames[currency]
            val market = "KRW-$currency"

            val balance = account.balance?.toDoubleOrNull() ?: 0.0
            val locked = account.locked?.toDoubleOrNull() ?: 0.0
            val totalQty = balance + locked
            val avgBuyPrice = account.avg_buy_price?.toDoubleOrNull() ?: 0.0
            val curPrice = ticker?.trade_price ?: 0.0
            val prevClosePrice = ticker?.prev_closing_price ?: 0.0

            val purAmt = avgBuyPrice * totalQty
            val evltAmt = curPrice * totalQty
            val evltPl = evltAmt - purAmt
            val prftRt = if (purAmt > 0) (evltPl / purAmt) * 100 else 0.0

            totPurAmt += purAmt
            totEvltAmt += evltAmt

            HoldingItem(
                stkCd = market,
                stkNm = marketInfo?.korean_name ?: currency,
                curPrc = curPrice.toLong().toString(),
                purPric = avgBuyPrice.toLong().toString(),
                purAmt = purAmt.toLong().toString(),
                evltAmt = evltAmt.toLong().toString(),
                evltvPrft = evltPl.toLong().toString(),
                prftRt = String.format("%.2f", prftRt),
                rmndQty = String.format("%.8f", totalQty),
                possRt = "0",
                predClosePric = prevClosePrice.toLong().toString(),
            )
        }

        // 보유 비중 계산
        val holdingListWithPossRt = holdingList.map { item ->
            val evltAmt = item.evltAmt.toDoubleOrNull() ?: 0.0
            val possRt = if (totEvltAmt > 0) (evltAmt / totEvltAmt) * 100 else 0.0
            item.copy(possRt = String.format("%.2f", possRt))
        }

        val totEvltPl = totEvltAmt - totPurAmt
        val totPrftRt = if (totPurAmt > 0) (totEvltPl / totPurAmt) * 100 else 0.0

        // DB 동기화
        memberHoldingSyncService.sync(
            memberId = memberId,
            holdings = holdingListWithPossRt.map { it.stkCd to it.stkNm },
            broker = upbitBroker
        )

        // 사용자 정렬 순서 반영
        val memberHoldings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, upbitBroker.id)
        val holdingMap = holdingListWithPossRt.associateBy { it.stkCd }
        val sortedHoldingList = memberHoldings.mapNotNull { mh ->
            holdingMap[mh.stkCd]?.copy(id = mh.id)
        }

        return HoldingListRes(
            totPurAmt = totPurAmt.toLong().toString(),
            totEvltAmt = totEvltAmt.toLong().toString(),
            totEvltPl = totEvltPl.toLong().toString(),
            totPrftRt = String.format("%.2f", totPrftRt),
            balance = krwAccount?.balance ?: "0",
            holdingList = sortedHoldingList
        )
    }

    fun streamCryptoHoldings(req: HoldingStreamReq) {
        cryptoStreamClient.cryptoListStream(req.items)
    }

    @Transactional
    fun reorderCryptoHoldings(req: HoldingReorderReq) {
        val memberId = getMemberId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        req.orderedIds.forEachIndexed { index, holdingId ->
            val holding = memberHoldingRepository.findByMemberIdAndId(memberId, holdingId)
                ?: throw IllegalArgumentException("보유코인을 찾을 수 없습니다.")
            holding.displayOrder = index
        }
    }

    private fun getLoginId(): String? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.username
    }

    private fun getMemberId(): Long? {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id
    }
}
