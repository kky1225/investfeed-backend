package com.example.investfeed.domain.investor.service

import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeOpenMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketItemList
import com.example.investfeed.kiwoom.rank.client.RankClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList
import kotlin.String

@Service
class InvestorService(
    private val rankClient: RankClient,
    private val priceClient: PriceClient,
) {
    fun investorList(
        req: InvestorListReq
    ): InvestorListRes? {
        val investorTradeDailyList: MutableList<InvestorListItem> = mutableListOf()
        var result: MutableList<KiwoomInvestorTradeOpenMarketItemList>

        when (req.orgnTp) {
            "6" -> {
                var kiwoomInvestorTradeDailyRes1 = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "1",
                    )
                )

                var kiwoomInvestorTradeDailyRes2 = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "1",
                        smtm_netprps_tp = "1",
                        stex_tp = "1",
                    )
                )

                val combinedList = (kiwoomInvestorTradeDailyRes1.opmr_invsr_trde ?: emptyList()) + (kiwoomInvestorTradeDailyRes2.opmr_invsr_trde ?: emptyList())
                result = combinedList.groupBy { it.stk_cd }.map { (_, items) ->
                        val totalAmt = items.sumOf { item ->
                            val netprps_amt = item.netprps_amt?.trim()?.toLongOrNull() ?: 0L

                            if (netprps_amt == 0L) {
                                val buy = item.buy_amt?.trim()?.toLongOrNull() ?: 0L
                                val sell = item.sell_amt?.trim()?.replace("--", "")?.toLongOrNull() ?: 0L
                                buy - sell
                            } else {
                                netprps_amt
                            }
                        }

                        items.first().apply {
                            this.netprps_amt = totalAmt.toString()
                        }
                    }.toMutableList()
                when (req.trdeTp) {
                    "1" -> {
                        result = result.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    }
                    "2" -> {
                        result = result.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    }
                }

                if (kiwoomInvestorTradeDailyRes1.return_code == 0 && kiwoomInvestorTradeDailyRes2.return_code == 0) {
                    result.forEach {
                        investorTradeDailyList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc,
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
                                buyAmt = it.buy_amt,
                                sellAmt = it.sell_amt,
                                netprpsQty = it.netprps_qty,
                                buyQty = it.buy_qty,
                                sellQty = it.sell_qty,
                            )
                        )
                    }
                }
            }
            "7" -> {
                val kiwoomInvestorTradeDailyRes = priceClient.investorTradeOpenMarket(
                    req = KiwoomInvestorTradeOpenMarketReq(
                        mrkt_tp = "000",
                        amt_qty_tp = "1",
                        invsr = req.orgnTp,
                        frgn_all = "0",
                        smtm_netprps_tp = "1",
                        stex_tp = "3",
                    )
                )

                if (kiwoomInvestorTradeDailyRes.return_code == 0) {
                    result = kiwoomInvestorTradeDailyRes.opmr_invsr_trde?.map {
                        val netprps_amt = it.netprps_amt?.trim()?.toLongOrNull() ?: 0L

                        if (netprps_amt == 0L) {
                            val buy = it.buy_amt?.trim()?.toLongOrNull() ?: 0L
                            val sell = it.sell_amt?.trim()?.replace("--", "")?.toLongOrNull() ?: 0L

                            it.apply { this.netprps_amt = (buy - sell).toString() }
                        } else {
                            it.apply { this.netprps_amt = netprps_amt.toString() }
                        }
                    }?.toMutableList() ?: emptyList()

                    when (req.trdeTp) {
                        "1" -> {
                            result = result.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                        }
                        "2" -> {
                            result = result.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                        }
                    }

                    result.forEach {
                        investorTradeDailyList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc,
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
                                buyAmt = it.buy_amt,
                                sellAmt = it.sell_amt,
                                netprpsQty = it.netprps_qty,
                                buyQty = it.buy_qty,
                                sellQty = it.sell_qty,
                            )
                        )
                    }
                }
            }
        }

        return InvestorListRes(
            investorList = investorTradeDailyList
        )
    }

//    fun investorList(
//        req: InvestorListReq
//    ): InvestorListRes {
//        val KiwoomInvestorTradeRes = rankClient.investorTrade(
//            req = KiwoomInvestorTradeReq(
//                mrkt_tp = "000",
//                amt_qty_tp = req.amtQtyTp,
//                qry_dt_tp = "0",
//                date = today("yyyyMMdd"),
//                stex_tp = "3",
//            )
//        )
//
//        val investorTradeDailyList = mutableListOf<InvestorListItem>()
//        if (KiwoomInvestorTradeRes.return_code == 0) {
//            KiwoomInvestorTradeRes.frgnr_orgn_trde_upper?.forEach {
//                investorTradeDailyList.add(
//                    InvestorListItem(
//                        forNetslmtStkCd = it.for_netslmt_stk_cd,
//                        forNetslmtStkNm = it.for_netslmt_stk_nm,
//                        forNetslmtAmt = it.for_netslmt_amt,
//                        forNetslmtQty = it.for_netslmt_qty,
//                        forNetprpsStkCd = it.for_netprps_stk_cd,
//                        forNetprpsStkNm = it.for_netprps_stk_nm,
//                        forNetprpsAmt = it.for_netprps_amt,
//                        forNetprpsQty = it.for_netprps_qty,
//                        orgnNetslmtStkCd = it.orgn_netslmt_stk_cd,
//                        orgnNetslmtStkNm = it.orgn_netslmt_stk_nm,
//                        orgnNetslmtAmt = it.orgn_netslmt_amt,
//                        orgnNetslmtQty = it.orgn_netslmt_qty,
//                        orgnNetprpsStkCd = it.orgn_netprps_stk_cd,
//                        orgnNetprpsStkNm = it.orgn_netprps_stk_nm,
//                        orgnNetprpsAmt = it.orgn_netprps_amt,
//                        orgnNetprpsQty = it.orgn_netprps_qty,
//                    )
//                )
//            }
//        }
//
//        return InvestorListRes(
//            stockInvestorList = investorTradeDailyList
//        )
//    }

    fun today(
        pattern: String
    ): String {
        val now = LocalDate.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}