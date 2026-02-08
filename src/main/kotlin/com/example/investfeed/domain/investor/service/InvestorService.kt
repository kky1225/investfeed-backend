package com.example.investfeed.domain.investor.service

import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.kiwoom.rank.client.RankClient
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomInvestorTradeReq
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.String

@Service
class InvestorService(
    private val rankClient: RankClient,
) {
    fun investorList(
        req: InvestorListReq
    ): InvestorListRes {
        val KiwoomInvestorTradeRes = rankClient.investorTrade(
            req = KiwoomInvestorTradeReq(
                mrkt_tp = "000",
                amt_qty_tp = req.amtQtyTp,
                qry_dt_tp = "0",
                date = today("yyyyMMdd"),
                stex_tp = "3",
            )
        )

        val investorTradeDailyList = mutableListOf<InvestorListItem>()
        if (KiwoomInvestorTradeRes.return_code == 0) {
            KiwoomInvestorTradeRes.frgnr_orgn_trde_upper?.forEach {
                investorTradeDailyList.add(
                    InvestorListItem(
                        forNetslmtStkCd = it.for_netslmt_stk_cd,
                        forNetslmtStkNm = it.for_netslmt_stk_nm,
                        forNetslmtAmt = it.for_netslmt_amt,
                        forNetslmtQty = it.for_netslmt_qty,
                        forNetprpsStkCd = it.for_netprps_stk_cd,
                        forNetprpsStkNm = it.for_netprps_stk_nm,
                        forNetprpsAmt = it.for_netprps_amt,
                        forNetprpsQty = it.for_netprps_qty,
                        orgnNetslmtStkCd = it.orgn_netslmt_stk_cd,
                        orgnNetslmtStkNm = it.orgn_netslmt_stk_nm,
                        orgnNetslmtAmt = it.orgn_netslmt_amt,
                        orgnNetslmtQty = it.orgn_netslmt_qty,
                        orgnNetprpsStkCd = it.orgn_netprps_stk_cd,
                        orgnNetprpsStkNm = it.orgn_netprps_stk_nm,
                        orgnNetprpsAmt = it.orgn_netprps_amt,
                        orgnNetprpsQty = it.orgn_netprps_qty,
                    )
                )
            }
        }

        return InvestorListRes(
            stockInvestorList = investorTradeDailyList
        )
    }

    fun today(
        pattern: String
    ): String {
        val now = LocalDate.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}