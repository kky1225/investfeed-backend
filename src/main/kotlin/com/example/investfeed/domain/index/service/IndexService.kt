package com.example.investfeed.domain.index.service

import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartYearListReq
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.chart.service.SectChartService
import com.example.investfeed.kiwoom.gold.client.GoldClient
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.IndexDetailRes
import com.example.investfeed.domain.index.dto.res.IndexListRes
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexDailyReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val sectClient: SectClient,
    private val sectChartService: SectChartService,
    private val goldClient: GoldClient,
) {
    fun indexList(): IndexListRes? {
        return IndexListRes(
            kospiPriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
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
            kosdacPriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
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
            kospi200PriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
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
        val sectIndexDailyListRes = sectClient.sectIndexDailyList(
            req = KiwoomSectIndexDailyReq(
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
            sectPriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = req.inds_cd
                )
            ),
            chartListRes = chartListRes,
            sectInvestor = sectClient.sectInvestor(
                req = KiwoomSectInvestorReq(
                    mrkt_tp = if (req.inds_cd == "101") "1" else "0",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
        )
    }
}