package com.example.investfeed.domain.realizedpnl.service

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.realizedpnl.dto.req.ManualRealizedPnlCreateReq
import com.example.investfeed.domain.realizedpnl.dto.req.ManualRealizedPnlUpdateReq
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlListRes
import com.example.investfeed.domain.realizedpnl.entity.MemberRealizedPnl
import com.example.investfeed.domain.realizedpnl.entity.PnlSource
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManualRealizedPnlService(
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
) {
    private val log = KotlinLogging.logger {}

    fun listStockRealizedPnls(year: Int?, month: Int?): RealizedPnlListRes {
        val memberId = getMemberId()

        val items = when {
            year != null && month != null ->
                memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearAndMonthOrderByYearDescMonthDesc(memberId, MarketType.STOCK, year, month)
                    .filter { it.source == PnlSource.MANUAL }
            year != null ->
                memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearOrderByYearDescMonthDesc(memberId, MarketType.STOCK, year)
                    .filter { it.source == PnlSource.MANUAL }
            else ->
                memberRealizedPnlRepository.findByMemberIdAndBrokerMarketOrderByYearDescMonthDesc(memberId, MarketType.STOCK)
                    .filter { it.source == PnlSource.MANUAL }
        }

        val pnlItems = items.map { toItem(it) }
        return RealizedPnlListRes(
            items = pnlItems,
            totalRealizedPnl = pnlItems.sumOf { it.realizedPnl }
        )
    }

    @Transactional
    fun createManualStockPnl(req: ManualRealizedPnlCreateReq): RealizedPnlItem {
        val memberId = getMemberId()
        val memberBroker = memberBrokerRepository.findByMemberIdAndId(memberId, req.brokerId)
            ?: throw IllegalArgumentException("증권사를 찾을 수 없습니다.")

        if (memberBroker.broker.type != BrokerType.MANUAL) {
            throw IllegalArgumentException("수동 입력 증권사가 아닙니다.")
        }

        if (memberBroker.broker.market != MarketType.STOCK) {
            throw IllegalArgumentException("주식 증권사가 아닙니다.")
        }

        val existing = memberRealizedPnlRepository.findByMemberIdAndBrokerIdAndYearAndMonth(
            memberId, memberBroker.broker.id, req.year, req.month
        )

        if (existing != null) {
            throw IllegalArgumentException("해당 기간의 실현손익이 이미 등록되어 있습니다.")
        }

        val pnl = memberRealizedPnlRepository.save(
            MemberRealizedPnl(
                memberId = memberId,
                broker = memberBroker.broker,
                year = req.year,
                month = req.month,
                realizedPnl = req.realizedPnl,
                source = PnlSource.MANUAL
            )
        )

        return toItem(pnl)
    }

    @Transactional
    fun updateManualStockPnl(id: Long, req: ManualRealizedPnlUpdateReq): RealizedPnlItem {
        val memberId = getMemberId()
        val pnl = memberRealizedPnlRepository.findByMemberIdAndId(memberId, id)
            ?: throw IllegalArgumentException("실현손익 데이터를 찾을 수 없습니다.")

        if (pnl.source != PnlSource.MANUAL) {
            throw IllegalArgumentException("수동 등록 데이터만 수정할 수 있습니다.")
        }

        pnl.realizedPnl = req.realizedPnl
        pnl.updatedAt = LocalDateTime.now()

        return toItem(pnl)
    }

    @Transactional
    fun deleteManualStockPnl(id: Long) {
        val memberId = getMemberId()
        val pnl = memberRealizedPnlRepository.findByMemberIdAndId(memberId, id)
            ?: throw IllegalArgumentException("실현손익 데이터를 찾을 수 없습니다.")

        if (pnl.source != PnlSource.MANUAL) {
            throw IllegalArgumentException("수동 등록 데이터만 삭제할 수 있습니다.")
        }

        memberRealizedPnlRepository.delete(pnl)
    }

    private fun toItem(pnl: MemberRealizedPnl): RealizedPnlItem {
        return RealizedPnlItem(
            id = pnl.id,
            brokerName = pnl.broker.name,
            brokerId = pnl.broker.id,
            market = pnl.broker.market.name,
            year = pnl.year,
            month = pnl.month,
            realizedPnl = pnl.realizedPnl,
            totalBuyAmt = pnl.totalBuyAmt,
            totalSellAmt = pnl.totalSellAmt,
            tradeFee = pnl.tradeFee,
            tradeTax = pnl.tradeTax,
            source = pnl.source.name
        )
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
