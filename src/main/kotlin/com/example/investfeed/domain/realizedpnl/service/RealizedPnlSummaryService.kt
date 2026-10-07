package com.example.investfeed.domain.realizedpnl.service

import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlSyncReq
import com.example.investfeed.domain.realizedpnl.dto.res.BrokerRealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlDashboardItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RealizedPnlSummaryService(
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val memberBrokerRepository: MemberBrokerRepository,
    private val stockRealizedPnlService: StockRealizedPnlService,
) {
    private val log = KotlinLogging.logger {}

    /**
     * 국내주식 실현손익 (증권사·월별). 수동 입력 증권사는 DB 저장값, 키움은 저장하지 않고 키움 API로 즉시 조회해 이어 붙인다.
     */
    fun stockItems(memberId: Long, year: Int, month: Int?): List<RealizedPnlItem> {
        val manual = (if (month != null) memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearAndMonthOrderByYearDescMonthDesc(memberId, MarketType.STOCK, year, month)
            else memberRealizedPnlRepository.findByMemberIdAndBrokerMarketAndYearOrderByYearDescMonthDesc(memberId, MarketType.STOCK, year))
            .map {
                RealizedPnlItem(
                    id = it.id, brokerName = it.broker.name, brokerId = it.broker.id,
                    market = it.broker.market.name, year = it.year, month = it.month,
                    realizedPnl = it.realizedPnl, totalBuyAmt = it.totalBuyAmt,
                    totalSellAmt = it.totalSellAmt, tradeFee = it.tradeFee,
                    tradeTax = it.tradeTax, source = it.source.name
                )
            }
        val kiwoom = runCatching { stockRealizedPnlService.syncStockRealizedPnls(RealizedPnlSyncReq(year, month)).items }
            .onFailure { log.error(it) { "키움 실현손익 조회 실패 memberId=$memberId $year-${month ?: "전체"}" } }
            .getOrDefault(emptyList())
        return manual + kiwoom
    }


    fun getDashboardSummary(): RealizedPnlDashboardItem {
        val memberId = getMemberId()
        val now = LocalDate.now()

        // 수동 데이터 (DB) - 브로커별로 그룹핑
        val manualItems = memberRealizedPnlRepository.findByMemberId(memberId)

        // API 데이터 (키움 API 1번 호출로 전체 조회)
        val apiAllItems = stockRealizedPnlService.syncStockRealizedPnls(RealizedPnlSyncReq()).items

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
