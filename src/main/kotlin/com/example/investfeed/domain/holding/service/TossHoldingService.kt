package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.auth.exception.ApiKeyNotFoundException
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.dto.res.HoldingItem
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.toss.account.client.TossAccountClient
import com.example.investfeed.toss.exchangerate.client.TossExchangeRateClient
import com.example.investfeed.toss.holding.TossSymbolMapper
import com.example.investfeed.toss.holding.client.TossHoldingClient
import com.example.investfeed.toss.holding.dto.res.TossHoldingItem
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class TossHoldingService(
    private val tossAccountClient: TossAccountClient,
    private val tossHoldingClient: TossHoldingClient,
    private val tossExchangeRateClient: TossExchangeRateClient,
    private val memberHoldingSyncService: MemberHoldingSyncService,
    private val brokerRepository: BrokerRepository,
    private val memberHoldingRepository: MemberHoldingRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val BROKER_NAME = "토스증권"
    }

    private data class Valuation(val curPrc: Long, val purPric: Long, val evltAmt: Long, val purAmt: Long)

    fun listTossHoldings(): HoldingListRes {
        val loginId = getLoginId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val memberId = getMemberId() ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")

        val tossBroker = brokerRepository.findByName(BROKER_NAME)
            ?: throw IllegalArgumentException("토스증권을 찾을 수 없습니다.")

        memberApiKeyRepository.findByMemberLoginIdAndBrokerId(loginId, tossBroker.id)
            ?: throw ApiKeyNotFoundException()

        val balance = memberBrokerRepository.findByMemberIdAndBrokerId(memberId, tossBroker.id)?.balance ?: 0L

        val accountSeq = resolveAccountSeq()
        val items = accountSeq?.let { tossHoldingClient.getHoldings(it)?.items } ?: emptyList()

        if (items.isEmpty()) {
            memberHoldingSyncService.sync(memberId, emptyList(), tossBroker)
            return HoldingListRes("0", "0", "0", "0", balance.toString(), emptyList())
        }

        // 평가값은 토스 응답 그대로 사용(토스 앱과 동일). 미국분만 USD→KRW 환율 환산.
        val usdKrwRate = if (items.any { TossSymbolMapper.isUs(it.marketCountry) }) fetchUsdKrwRate() else 0.0

        var totEvltAmt = 0L
        var totPurAmt = 0L

        val holdingList = items.mapNotNull { item ->
            val symbol = item.symbol ?: return@mapNotNull null
            val stkCd = TossSymbolMapper.toStkCd(symbol, item.marketCountry)
            val v = evaluate(item, usdKrwRate)

            totEvltAmt += v.evltAmt
            totPurAmt += v.purAmt
            val evltPl = v.evltAmt - v.purAmt
            val prftRt = if (v.purAmt > 0) evltPl.toDouble() / v.purAmt * 100 else 0.0

            HoldingItem(
                stkCd = stkCd,
                stkNm = item.name ?: symbol,
                curPrc = v.curPrc.toString(),
                purPric = v.purPric.toString(),
                purAmt = v.purAmt.toString(),
                evltAmt = v.evltAmt.toString(),
                evltvPrft = evltPl.toString(),
                prftRt = String.format("%.2f", prftRt),
                rmndQty = item.quantity ?: "0",
                possRt = "0",
                predClosePric = "0",
            )
        }

        val holdingListWithPossRt = holdingList.map { hi ->
            val evlt = hi.evltAmt.toLongOrNull() ?: 0L
            val possRt = if (totEvltAmt > 0) evlt.toDouble() / totEvltAmt * 100 else 0.0
            hi.copy(possRt = String.format("%.2f", possRt))
        }

        val totEvltPl = totEvltAmt - totPurAmt
        val totPrftRt = if (totPurAmt > 0) totEvltPl.toDouble() / totPurAmt * 100 else 0.0

        memberHoldingSyncService.sync(
            memberId = memberId,
            holdings = holdingListWithPossRt.map { it.stkCd to it.stkNm },
            broker = tossBroker
        )

        val memberHoldings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, tossBroker.id)
        val holdingMap = holdingListWithPossRt.associateBy { it.stkCd }
        val sortedHoldingList = memberHoldings.mapNotNull { mh ->
            holdingMap[mh.stkCd]?.copy(id = mh.id)
        }

        return HoldingListRes(
            totPurAmt = totPurAmt.toString(),
            totEvltAmt = totEvltAmt.toString(),
            totEvltPl = totEvltPl.toString(),
            totPrftRt = String.format("%.2f", totPrftRt),
            balance = balance.toString(),
            holdingList = sortedHoldingList
        )
    }

    private fun evaluate(
        item: TossHoldingItem,
        usdKrwRate: Double,
    ): Valuation {
        val itemPurAmt = item.marketValue?.purchaseAmount?.toDoubleOrNull() ?: 0.0
        val itemEvltAmt = item.marketValue?.amount?.toDoubleOrNull() ?: 0.0
        val avgPrice = item.averagePurchasePrice?.toDoubleOrNull() ?: 0.0
        val lastPrice = item.lastPrice?.toDoubleOrNull() ?: 0.0

        val rate = if (TossSymbolMapper.isUs(item.marketCountry)) usdKrwRate else 1.0
        return Valuation(
            curPrc = (lastPrice * rate).toLong(),
            purPric = (avgPrice * rate).toLong(),
            evltAmt = (itemEvltAmt * rate).toLong(),
            purAmt = (itemPurAmt * rate).toLong(),
        )
    }

    private fun resolveAccountSeq(): Long? {
        val accounts = tossAccountClient.getAccounts()
        return (accounts.firstOrNull { it.accountType == "BROKERAGE" } ?: accounts.firstOrNull())?.accountSeq
    }

    private fun fetchUsdKrwRate(): Double {
        return try {
            tossExchangeRateClient.getRate("USD", "KRW")?.result?.rate?.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            log.warn { "토스 USD/KRW 환율 조회 실패: ${e.message}" }
            0.0
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
