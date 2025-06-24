package com.example.investfeed.kiwoom.dashboard.service

import com.example.investfeed.kiwoom.dashboard.dto.res.DashboardRes
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeRankListReq
import com.example.investfeed.kiwoom.investor.service.InvestorService
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectIndexDailyListReq
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectIndexListReq
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectPriceNowReq
import com.example.investfeed.kiwoom.sect.service.SectService
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val sectService: SectService,
    private val investorService: InvestorService,
) {
    private val log = KotlinLogging.logger {}

    fun dashboard(
        req: SectIndexListReq,
    ): DashboardRes? {
        val kospiIndexDailyListRes = sectService.sectIndexDailyList(
            req = SectIndexDailyListReq(
                mrkt_tp = "0",
                inds_cd = "001"
            )
        )

        return DashboardRes(
            kospiPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "001"
                )
            ),
            kospiIndexDailyListRes = kospiIndexDailyListRes,
            kospiInvestor = sectService.sectInvestor(
                req = SectInvestorReq(
                    mrkt_tp = "0",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
            kosdacPriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "1",
                    inds_cd = "101"
                )
            ),
            kosdacIndexDailyListRes = sectService.sectIndexDailyList(
                req = SectIndexDailyListReq(
                    mrkt_tp = "1",
                    inds_cd = "101"
                )
            ),
            kosdacInvestor = sectService.sectInvestor(
                req = SectInvestorReq(
                    mrkt_tp = "1",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
            kospi200PriceRes = sectService.sectPriceNow(
                req = SectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "201"
                )
            ),
            kospi200IndexDailyListRes = sectService.sectIndexDailyList(
                req = SectIndexDailyListReq(
                    mrkt_tp = "0",
                    inds_cd = "201"
                )
            ),
            investorTradeRankRes = investorService.investorTradeRankList(
                req = InvestorTradeRankListReq(
                    dt = "0",
                    strt_dt = kospiIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n,
                    end_dt = kospiIndexDailyListRes?.inds_cur_prc_daly_rept?.get(0)?.dt_n,
                    mrkt_tp = "001",
                    stk_inds_tp = "0",
                    amt_qty_tp = "0",
                    stex_tp = "1"
                )
            )
        )
    }
}