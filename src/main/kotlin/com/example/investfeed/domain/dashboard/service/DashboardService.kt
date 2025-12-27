package com.example.investfeed.domain.dashboard.service

import com.example.investfeed.domain.dashboard.dto.res.DashboardRes
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeRankListReq
import com.example.investfeed.kiwoom.investor.service.InvestorService
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexDailyReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val sectClient: SectClient,
    private val investorService: InvestorService,
) {
    private val log = KotlinLogging.logger {}

    fun dashboard(): DashboardRes? {
        log.debug { "dashboard" }

        val kospiIndexDailyListRes = sectClient.sectIndexDailyList(
            req = KiwoomSectIndexDailyReq(
                mrkt_tp = "0",
                inds_cd = "001"
            )
        )

        return DashboardRes(
            kospiPriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "001"
                )
            ),
            kospiIndexDailyListRes = kospiIndexDailyListRes,
            kospiInvestor = sectClient.sectInvestor(
                req = KiwoomSectInvestorReq(
                    mrkt_tp = "0",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
            kosdacPriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "1",
                    inds_cd = "101"
                )
            ),
            kosdacIndexDailyListRes = sectClient.sectIndexDailyList(
                req = KiwoomSectIndexDailyReq(
                    mrkt_tp = "1",
                    inds_cd = "101"
                )
            ),
            kosdacInvestor = sectClient.sectInvestor(
                req = KiwoomSectInvestorReq(
                    mrkt_tp = "1",
                    amt_qty_tp = "0",
                    stex_tp = "0"
                )
            ),
            kospi200PriceRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = "201"
                )
            ),
            kospi200IndexDailyListRes = sectClient.sectIndexDailyList(
                req = KiwoomSectIndexDailyReq(
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