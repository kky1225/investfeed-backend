package com.example.investfeed.domain.realizedpnl.service

import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlSyncReq
import com.example.investfeed.domain.realizedpnl.dto.res.BrokerRealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.MonthlyPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlDashboardItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlSummaryRes
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RealizedPnlSummaryService(
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val stockRealizedPnlService: StockRealizedPnlService,
) {

    fun getSummary(year: Int?): RealizedPnlSummaryRes {
        // 수동 데이터 (DB)
        val memberId = getMemberId()
        val manualItems = if (year != null) {
            memberRealizedPnlRepository.findByMemberIdAndYear(memberId, year)
        } else {
            memberRealizedPnlRepository.findByMemberId(memberId)
        }
        val manualPnlItems = manualItems.map {
            RealizedPnlItem(
                id = it.id, brokerName = it.broker.name, brokerId = it.broker.id,
                market = it.broker.market.name, year = it.year, month = it.month,
                realizedPnl = it.realizedPnl, totalBuyAmt = it.totalBuyAmt,
                totalSellAmt = it.totalSellAmt, tradeFee = it.tradeFee,
                tradeTax = it.tradeTax, source = it.source.name
            )
        }

        // API 데이터 (키움 API 직접 호출)
        val apiPnlItems = stockRealizedPnlService.fetchApiPnl(RealizedPnlSyncReq(year = year)).items

        // 합산
        val allItems = manualPnlItems + apiPnlItems

        val monthlyMap = allItems.groupBy { Pair(it.year, it.month) }
        val monthly = monthlyMap.map { (key, items) ->
            val stockPnl = items.filter { it.market == MarketType.STOCK.name }.sumOf { it.realizedPnl }
            val cryptoPnl = items.filter { it.market == MarketType.CRYPTO.name }.sumOf { it.realizedPnl }
            MonthlyPnlItem(
                year = key.first, month = key.second,
                stockPnl = stockPnl, cryptoPnl = cryptoPnl,
                totalPnl = stockPnl + cryptoPnl
            )
        }.sortedWith(compareBy({ it.year }, { it.month }))

        val totalPnl = allItems.sumOf { it.realizedPnl }
        val stockTotal = allItems.filter { it.market == MarketType.STOCK.name }.sumOf { it.realizedPnl }
        val cryptoTotal = allItems.filter { it.market == MarketType.CRYPTO.name }.sumOf { it.realizedPnl }

        return RealizedPnlSummaryRes(
            monthly = monthly,
            yearlyTotal = totalPnl,
            allTimeTotal = totalPnl,
            stockTotal = stockTotal,
            cryptoTotal = cryptoTotal
        )
    }

    fun getDashboardSummary(): RealizedPnlDashboardItem {
        val memberId = getMemberId()
        val now = LocalDate.now()

        // 수동 데이터 (DB) - 브로커별로 그룹핑
        val manualItems = memberRealizedPnlRepository.findByMemberId(memberId)

        // API 데이터 (키움 API 1번 호출로 전체 조회)
        val apiAllItems = stockRealizedPnlService.fetchApiPnl(RealizedPnlSyncReq()).items

        // 전체 합산
        val allItems = manualItems.map {
            RealizedPnlItem(
                id = it.id, brokerName = it.broker.name, brokerId = it.broker.id,
                market = it.broker.market.name, year = it.year, month = it.month,
                realizedPnl = it.realizedPnl, totalBuyAmt = it.totalBuyAmt,
                totalSellAmt = it.totalSellAmt, tradeFee = it.tradeFee,
                tradeTax = it.tradeTax, source = it.source.name
            )
        } + apiAllItems

        val totalCurrentMonth = allItems.filter { it.year == now.year && it.month == now.monthValue }.sumOf { it.realizedPnl }
        val totalYtd = allItems.filter { it.year == now.year }.sumOf { it.realizedPnl }
        val totalAllTime = allItems.sumOf { it.realizedPnl }

        // 증권사/거래소별 집계 (모든 등록된 브로커 포함)
        val allBrokers = memberBrokerRepository.findByMemberIdOrderByOrderIndex(memberId)
        val pnlByBroker = allItems.groupBy { it.brokerId }

        val brokerPnlList = allBrokers.map { memberBroker ->
            val items = pnlByBroker[memberBroker.broker.id] ?: emptyList()
            BrokerRealizedPnlItem(
                brokerName = memberBroker.broker.name,
                brokerId = memberBroker.broker.id,
                market = memberBroker.broker.market.name,
                currentMonthPnl = items.filter { it.year == now.year && it.month == now.monthValue }.sumOf { it.realizedPnl },
                ytdPnl = items.filter { it.year == now.year }.sumOf { it.realizedPnl },
                allTimePnl = items.sumOf { it.realizedPnl }
            )
        }

        return RealizedPnlDashboardItem(
            currentMonthPnl = totalCurrentMonth,
            ytdPnl = totalYtd,
            allTimePnl = totalAllTime,
            brokerPnlList = brokerPnlList
        )
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
