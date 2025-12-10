package com.example.investfeed.kiwoom.index.service

import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.*
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.chart.service.SectChartService
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowReq
import com.example.investfeed.kiwoom.gold.client.GoldClient
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
    private val sectChartService: SectChartService,
    private val goldClient: GoldClient,
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
            goldPriceRes = goldClient.goldPriceNow(
                req = GoldPriceNowReq(
                    stk_cd = "M04020000"
                )
            ),
            goldChartMinuteListRes = goldClient.goldChartMinuteList(
                req = GoldChartMinuteListReq(
                    stk_cd = "M04020000",
                    tic_scope = "1",
                )
            )
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
            IndexChartType.DAY -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartDayList(
                        req = SectChartDayListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            IndexChartType.WEEK -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartWeekList(
                        req = SectChartWeekListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            IndexChartType.MONTH -> {
                chartListRes = sectIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n?.let {
                    sectChartService.sectChartMonthList(
                        req = SectChartMonthListReq(
                            inds_cd = req.inds_cd,
                            base_dt = it
                        )
                    )
                }
            }
            IndexChartType.YEAR -> {
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