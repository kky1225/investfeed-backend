package com.example.investfeed.kiwoom.index.service

import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartYearListReq
import com.example.investfeed.kiwoom.chart.enum.ChartType
import com.example.investfeed.kiwoom.chart.service.SectChartService
import com.example.investfeed.kiwoom.index.dto.req.IndexDetailReq
import com.example.investfeed.kiwoom.index.dto.res.IndexDetailRes
import com.example.investfeed.kiwoom.index.dto.res.IndexListRes
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectIndexDailyListReq
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectPriceNowReq
import com.example.investfeed.kiwoom.sect.service.SectService
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val sectService: SectService,
    private val sectChartService: SectChartService
) {
    fun indexList(): IndexListRes? {
        return IndexListRes(
            kospiPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "001"
                )
            ),
            kospiChartMinuteListRes = sectChartService.sectChartMinuteList(
                req = SectChartMinuteListReq(
                    inds_cd = "001",
                    tic_scope = "1"
                )
            ),
            kosdacPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "101"
                )
            ),
            kosdacChartMinuteListRes = sectChartService.sectChartMinuteList(
                req = SectChartMinuteListReq(
                    inds_cd = "101",
                    tic_scope = "1"
                )
            ),
            kospi200PriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "201"
                )
            ),
            kospi200ChartMinuteListRes = sectChartService.sectChartMinuteList(
                req = SectChartMinuteListReq(
                    inds_cd = "201",
                    tic_scope = "1"
                )
            ),
        )
    }

    fun indexDetail(
        req: IndexDetailReq
    ): IndexDetailRes<*>? {
        val sectIndexDailyListRes = sectService.sectIndexDailyList(
            req = SectIndexDailyListReq(
                mrkt_tp = "0",
                inds_cd = req.inds_cd
            )
        )

        var chartListRes: Any?

        when(req.chart_type) {
            ChartType.DAY -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartDayList(
                        req = SectChartDayListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            ChartType.WEEK -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartWeekList(
                        req = SectChartWeekListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            ChartType.MONTH -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartMonthList(
                        req = SectChartMonthListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            ChartType.YEAR -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartYearList(
                        req = SectChartYearListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            else -> {
                chartListRes = req.chart_type.value?.let {
                    sectChartService.sectChartMinuteList(
                        req = SectChartMinuteListReq(
                            inds_cd = req.inds_cd,
                            tic_scope = it
                        )
                    )
                }
            }
        }

        return IndexDetailRes(
            sectPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = req.inds_cd
                )
            ),
            chartListRes = chartListRes,
            sectInvestor = sectService.sectInvestor(
                req = SectInvestorReq(
                    mrkt_tp = if(req.inds_cd == "101") "1" else "0",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
        )
    }
}