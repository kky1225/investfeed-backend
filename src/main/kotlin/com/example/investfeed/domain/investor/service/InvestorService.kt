package com.example.investfeed.domain.investor.service

import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.kiwoom.rank.client.RankClient
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomInvestorTradeDailyReq
import org.springframework.stereotype.Service

@Service
class InvestorService(
    private val rankClient: RankClient,
) {
    fun investorList(
        req: InvestorListReq
    ): InvestorListRes {
        val kiwoomInvestorTradeDailyRes = rankClient.investorTradeDaily(
            req = KiwoomInvestorTradeDailyReq(
                trde_tp = req.trdeTp,
                mrkt_tp = "000",
                orgn_tp = req.orgnTp,
            )
        )

        val investorTradeDailyList = mutableListOf<InvestorListItem>()
        if (kiwoomInvestorTradeDailyRes.return_code == 0) {
            kiwoomInvestorTradeDailyRes.opmr_invsr_trde_upper?.forEach {
                investorTradeDailyList.add(
                    InvestorListItem(
                        stkCd = it.stk_cd,
                        stkNm = it.stk_nm,
                        selQty = it.sel_qty,
                        buyQty = it.buy_qty,
                        netslmt = it.netslmt,
                    )
                )
            }
        }

        return InvestorListRes(
            stockInvestorList = investorTradeDailyList
        )
    }
}