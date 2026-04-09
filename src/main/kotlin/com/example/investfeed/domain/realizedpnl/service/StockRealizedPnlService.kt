package com.example.investfeed.domain.realizedpnl.service

import com.example.investfeed.domain.holding.entity.BrokerType
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.repository.MemberBrokerRepository
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlSyncReq
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlListRes
import com.example.investfeed.domain.realizedpnl.entity.PnlSource
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.kiwoom.realizedpnl.client.RealizedPnlClient
import com.example.investfeed.kiwoom.realizedpnl.dto.req.KiwoomRealizedPnlReq
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class StockRealizedPnlService(
    private val memberBrokerRepository: MemberBrokerRepository,
    private val realizedPnlClient: RealizedPnlClient,
) {
    private val log = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun fetchApiPnl(req: RealizedPnlSyncReq): RealizedPnlListRes {
        val apiBrokers = memberBrokerRepository.findByMemberIdAndBrokerMarketOrderByOrderIndex(getMemberId(), MarketType.STOCK)
            .filter { it.broker.type == BrokerType.API }

        val resultItems = mutableListOf<RealizedPnlItem>()
        var idSeq = 1L
        val now = LocalDate.now()

        // 기간 범위 결정: 전체/연별/월별 모두 한번의 API 호출
        val startDate = when {
            req.year == null -> "20070801"
            req.month != null -> YearMonth.of(req.year, req.month).atDay(1).format(dateFormatter)
            else -> "${req.year}0101"
        }
        val endDate = when {
            req.year == null -> now.format(dateFormatter)
            req.month != null -> YearMonth.of(req.year, req.month).atEndOfMonth().format(dateFormatter)
            req.year == now.year -> now.format(dateFormatter)
            else -> "${req.year}1231"
        }

        for (memberBroker in apiBrokers) {
            try {
                val kiwoomRes = realizedPnlClient.realizedPnl(
                    KiwoomRealizedPnlReq(strt_dt = startDate, end_dt = endDate)
                ) ?: continue

                val dailyItems = (kiwoomRes.dt_rlzt_pl ?: emptyList())
                    .filter { daily ->
                        // 매수/매도/손익이 모두 0인 항목은 제외
                        val buyAmt = parseLong(daily.buy_amt)
                        val sellAmt = parseLong(daily.sell_amt)
                        val pnl = parseLong(daily.tdy_sel_pl)
                        buyAmt != 0L || sellAmt != 0L || pnl != 0L
                    }
                if (dailyItems.isEmpty()) continue

                // 일별 데이터를 연월별로 그룹핑 (키: "202401" 형태)
                val monthlyGroups = dailyItems.groupBy { daily ->
                    daily.dt?.substring(0, 6) ?: ""
                }.filterKeys { it.isNotEmpty() }

                for ((yearMonthKey, monthDailyItems) in monthlyGroups) {
                    val groupYear = yearMonthKey.substring(0, 4).toInt()
                    val groupMonth = yearMonthKey.substring(4, 6).toInt()
                    val monthPnl = monthDailyItems.sumOf { parseLong(it.tdy_sel_pl) }
                    val monthBuyAmt = monthDailyItems.sumOf { parseLong(it.buy_amt) }
                    val monthSellAmt = monthDailyItems.sumOf { parseLong(it.sell_amt) }
                    val monthFee = monthDailyItems.sumOf { parseLong(it.tdy_trde_cmsn) }
                    val monthTax = monthDailyItems.sumOf { parseLong(it.tdy_trde_tax) }

                    resultItems.add(
                        RealizedPnlItem(
                            id = idSeq++,
                            brokerName = memberBroker.broker.name,
                            brokerId = memberBroker.broker.id,
                            market = memberBroker.broker.market.name,
                            year = groupYear,
                            month = groupMonth,
                            realizedPnl = monthPnl,
                            totalBuyAmt = monthBuyAmt,
                            totalSellAmt = monthSellAmt,
                            tradeFee = monthFee,
                            tradeTax = monthTax,
                            source = PnlSource.API.name
                        )
                    )
                }
            } catch (e: Exception) {
                log.error { "키움 실현손익 조회 실패 (${memberBroker.broker.name}): ${e.message}" }
            }
        }

        return RealizedPnlListRes(
            items = resultItems,
            totalRealizedPnl = resultItems.sumOf { it.realizedPnl }
        )
    }

    private fun parseLong(value: String?): Long {
        return value?.replace(",", "")?.trim()?.toLongOrNull() ?: 0
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
