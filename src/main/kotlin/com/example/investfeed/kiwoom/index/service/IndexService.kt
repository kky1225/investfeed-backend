package com.example.investfeed.kiwoom.index.service

import com.example.investfeed.kiwoom.chart.dto.index.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.service.ChartService
import com.example.investfeed.kiwoom.index.dto.res.IndexListRes
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectPriceNowReq
import com.example.investfeed.kiwoom.sect.service.SectService
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val sectService: SectService,
    private val chartService: ChartService
) {
    fun indexList(): IndexListRes? {
        return IndexListRes(
            kospiPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "001"
                )
            ),
            kospiChartMinuteListRes = chartService.sectChartMinuteList(
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
            kosdacChartMinuteListRes = chartService.sectChartMinuteList(
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
            kospi200ChartMinuteListRes = chartService.sectChartMinuteList(
                req = SectChartMinuteListReq(
                    inds_cd = "201",
                    tic_scope = "1"
                )
            ),
        )
    }
}