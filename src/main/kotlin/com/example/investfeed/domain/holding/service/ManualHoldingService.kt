package com.example.investfeed.domain.holding.service

import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingCreateReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingUpdateReq
import com.example.investfeed.domain.holding.dto.res.ManualHoldingItem
import com.example.investfeed.domain.holding.dto.res.ManualHoldingListRes
import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MemberHolding
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.holding.repository.MemberHoldingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInterestReq
import com.example.investfeed.kiwoom.stock.dto.res.KiwoomStockInterest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManualHoldingService(
    private val memberHoldingRepository: MemberHoldingRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val stockClient: StockClient,
    private val holidayService: HolidayService,
) {

    fun listManualHoldings(brokerId: Long): ManualHoldingListRes {
        val memberId = getMemberId()
        val memberBroker = memberBrokerRepository.findByMemberIdAndId(memberId, brokerId)
            ?: throw IllegalArgumentException("증권사를 찾을 수 없습니다.")

        if (memberBroker.broker.type != BrokerType.MANUAL) {
            throw IllegalArgumentException("수동 입력 증권사가 아닙니다.")
        }

        val holdings = memberHoldingRepository.findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId, memberBroker.broker.id)

        if (holdings.isEmpty()) {
            return ManualHoldingListRes(balance = memberBroker.balance, holdings = emptyList())
        }

        val priceMap = fetchCurrentPrices(holdings.map { it.stkCd })
        val isHoliday = holidayService.isHoliday()

        return ManualHoldingListRes(
            balance = memberBroker.balance,
            holdings = holdings.map { holding ->
                val price = priceMap[holding.stkCd]
                val curPrc = price?.cur_prc?.replace("^[+-]".toRegex(), "") ?: "0"
                ManualHoldingItem(
                    id = holding.id,
                    stkCd = holding.stkCd,
                    stkNm = holding.stkNm,
                    purPrice = holding.purPrice ?: 0,
                    quantity = holding.quantity ?: 0,
                    purAmt = holding.purAmt ?: 0,
                    curPrc = curPrc,
                    fluRt = price?.flu_rt ?: "0",
                    basePric = if (isHoliday) curPrc else price?.base_pric ?: "0"
                )
            }
        )
    }

    @Transactional
    fun createManualHolding(req: ManualHoldingCreateReq): ManualHoldingItem {
        val memberId = getMemberId()
        val memberBroker = memberBrokerRepository.findByMemberIdAndId(memberId, req.brokerId)
            ?: throw IllegalArgumentException("증권사를 찾을 수 없습니다.")

        if (memberBroker.broker.type != BrokerType.MANUAL) {
            throw IllegalArgumentException("수동 입력 증권사가 아닙니다.")
        }

        val nextOrder = memberHoldingRepository.findMaxDisplayOrder(memberId, memberBroker.broker.id) + 1

        val holding = memberHoldingRepository.save(
            MemberHolding(
                memberId = memberId,
                stkCd = req.stkCd,
                stkNm = req.stkNm,
                broker = memberBroker.broker,
                purPrice = req.purPrice,
                quantity = req.quantity,
                purAmt = req.purAmt,
                displayOrder = nextOrder,
                updatedAt = LocalDateTime.now()
            )
        )

        val priceMap = fetchCurrentPrices(listOf(holding.stkCd))
        val price = priceMap[holding.stkCd]

        return ManualHoldingItem(
            id = holding.id,
            stkCd = holding.stkCd,
            stkNm = holding.stkNm,
            purPrice = holding.purPrice ?: 0,
            quantity = holding.quantity ?: 0,
            purAmt = holding.purAmt ?: 0,
            curPrc = price?.cur_prc?.replace("^[+-]".toRegex(), "") ?: "0",
            fluRt = price?.flu_rt ?: "0",
            basePric = price?.base_pric ?: "0"
        )
    }

    @Transactional
    fun updateManualHolding(holdingId: Long, req: ManualHoldingUpdateReq): ManualHoldingItem {
        val memberId = getMemberId()
        val holding = memberHoldingRepository.findByMemberIdAndId(memberId, holdingId)
            ?: throw IllegalArgumentException("보유주식을 찾을 수 없습니다.")

        holding.purPrice = req.purPrice
        holding.quantity = req.quantity
        holding.purAmt = req.purAmt
        holding.updatedAt = LocalDateTime.now()

        val priceMap = fetchCurrentPrices(listOf(holding.stkCd))
        val price = priceMap[holding.stkCd]

        return ManualHoldingItem(
            id = holding.id,
            stkCd = holding.stkCd,
            stkNm = holding.stkNm,
            purPrice = holding.purPrice ?: 0,
            quantity = holding.quantity ?: 0,
            purAmt = holding.purAmt ?: 0,
            curPrc = price?.cur_prc?.replace("^[+-]".toRegex(), "") ?: "0",
            fluRt = price?.flu_rt ?: "0",
            basePric = price?.base_pric ?: "0"
        )
    }

    @Transactional
    fun deleteManualHolding(holdingId: Long) {
        val memberId = getMemberId()
        val holding = memberHoldingRepository.findByMemberIdAndId(memberId, holdingId)
            ?: throw IllegalArgumentException("보유주식을 찾을 수 없습니다.")

        memberHoldingRepository.delete(holding)
    }

    @Transactional
    fun reorderManualHoldings(req: HoldingReorderReq) {
        val memberId = getMemberId()
        req.orderedIds.forEachIndexed { index, holdingId ->
            val holding = memberHoldingRepository.findByMemberIdAndId(memberId, holdingId)
                ?: throw IllegalArgumentException("보유주식을 찾을 수 없습니다.")
            holding.displayOrder = index
        }
    }

    @Transactional
    fun updateBalance(memberBrokerId: Long, balance: Long): Long {
        val memberId = getMemberId()
        val memberBroker = memberBrokerRepository.findByMemberIdAndId(memberId, memberBrokerId)
            ?: throw IllegalArgumentException("증권사를 찾을 수 없습니다.")
        memberBroker.balance = balance
        return memberBroker.balance
    }

    private fun fetchCurrentPrices(stkCds: List<String>): Map<String, KiwoomStockInterest> {
        if (stkCds.isEmpty()) return emptyMap()

        val stkCdParam = stkCds.joinToString("|")
        val res = stockClient.stockInterest(KiwoomStockInterestReq(stk_cd = stkCdParam))

        return res.atn_stk_infr?.associateBy { it.stk_cd ?: "" } ?: emptyMap()
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
