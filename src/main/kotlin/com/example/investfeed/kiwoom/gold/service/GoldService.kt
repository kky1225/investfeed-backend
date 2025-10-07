package com.example.investfeed.kiwoom.gold.service

import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartWeekListReq
import com.example.investfeed.kiwoom.chart.enum.ChartType
import com.example.investfeed.kiwoom.chart.service.GoldChartService
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldDetailReq
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowReq
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldDetailRes
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class GoldService(
    private val goldClient: GoldClient,
    private val goldChartService: GoldChartService,
) {
    private val log = KotlinLogging.logger {}

    fun goldDetail(
        req: GoldDetailReq
    ): GoldDetailRes<*>? {
        var chartListRes: Any?

        val baseDt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        when(req.chart_type) {
            ChartType.DAY -> {
                chartListRes = goldChartService.goldChartDayList(
                    req = GoldChartDayListReq(
                        stk_cd = req.stk_cd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )
            }
            ChartType.WEEK -> {
                chartListRes = goldChartService.goldChartWeekList(
                    req = GoldChartWeekListReq(
                        stk_cd = req.stk_cd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )
            }
            ChartType.MONTH -> {
                chartListRes = goldChartService.goldChartMonthList(
                    req = GoldChartMonthListReq(
                        stk_cd = req.stk_cd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )
            }
            else -> {
                chartListRes = req.chart_type.value?.let {
                    goldChartService.goldChartMinuteList(
                        req = GoldChartMinuteListReq(
                            stk_cd = req.stk_cd,
                            tic_scope = it,
                            upd_stkpc_tp = "1"
                        )
                    )
                }
            }
        }

        return GoldDetailRes(
            goldPriceNowRes = goldClient.goldPriceNow(
                req = GoldPriceNowReq(
                    stk_cd = req.stk_cd
                )
            ),
            chartListRes = chartListRes,
            goldInvestor = goldClient.goldInvestor()
        )
    }
}