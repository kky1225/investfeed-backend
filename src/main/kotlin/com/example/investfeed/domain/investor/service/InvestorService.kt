package com.example.investfeed.domain.investor.service

import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.res.InvestorListItem
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeCloseMarketReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomInvestorTradeOpenMarketReq
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeCloseMarketItemList
import com.example.investfeed.kiwoom.price.dto.res.KiwoomInvestorTradeOpenMarketItemList
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.Collections.emptyList
import kotlin.streams.toList

@Service
class InvestorService(
    private val priceClient: PriceClient,
) {
    fun investorList(
        req: InvestorListReq
    ): InvestorListRes? {
        val investorList: MutableList<InvestorListItem> = mutableListOf()
        var openResult: MutableList<KiwoomInvestorTradeOpenMarketItemList> = mutableListOf()
        var closeResult: MutableList<KiwoomInvestorTradeCloseMarketItemList> = mutableListOf()

        if (isMarketAfter()) {
            val kiwoomInvestorTradeCloseMarketRes = priceClient.investorTradeCloseMarket(
                req = KiwoomInvestorTradeCloseMarketReq(
                    mrkt_tp = "000",
                    amt_qty_tp = "1",
                    trde_tp = "0",
                    stex_tp = "3",
                )
            )

            if (kiwoomInvestorTradeCloseMarketRes.return_code == 0) {
                when (req.orgnTp) {
                    "6" -> {
                        when (req.trdeTp) {
                            "1" -> {
                                kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedByDescending { it.frgnr_invsr?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { closeResult = it.toList() }
                            }
                            "2" -> {
                                kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedBy { it.frgnr_invsr?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { closeResult = it.toList() }
                            }
                        }
                    }
                    "7" -> {
                        when (req.trdeTp) {
                            "1" -> {
                                kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedByDescending { it.orgn?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { closeResult = it.toList() }
                            }
                            "2" -> {
                                kiwoomInvestorTradeCloseMarketRes.opaf_invsr_trde?.sortedBy { it.orgn?.toLongOrNull() ?: 0L }?.stream()?.limit(100)?.let { closeResult = it.toList() }
                            }
                        }
                    }
                }

                when (req.orgnTp) {
                    "6" -> {
                        closeResult.forEach {
                            investorList.add(
                                InvestorListItem(
                                    stkCd = it.stk_cd,
                                    stkNm = it.stk_nm,
                                    curPrc = it.cur_prc,
                                    preSig = it.pre_sig,
                                    predPre = it.pred_pre,
                                    fluRt = it.flu_rt,
                                    accTrdeQty = it.trde_qty,
                                    netprpsAmt = it.frgnr_invsr,
                                )
                            )
                        }
                    }
                    "7" -> {
                        closeResult.forEach {
                            investorList.add(
                                InvestorListItem(
                                    stkCd = it.stk_cd,
                                    stkNm = it.stk_nm,
                                    curPrc = it.cur_prc,
                                    preSig = it.pre_sig,
                                    predPre = it.pred_pre,
                                    fluRt = it.flu_rt,
                                    accTrdeQty = it.trde_qty,
                                    netprpsAmt = it.orgn,
                                )
                            )
                        }
                    }
                }
            }

            return InvestorListRes(
                investorList = investorList
            )
        }

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
                openResult = combinedList.groupBy { it.stk_cd }.map { (_, items) ->
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
                        openResult = openResult.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    }
                    "2" -> {
                        openResult = openResult.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                    }
                }

                if (kiwoomInvestorTradeDailyRes1.return_code == 0 && kiwoomInvestorTradeDailyRes2.return_code == 0) {
                    openResult.forEach {
                        investorList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc,
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
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
                    openResult = kiwoomInvestorTradeDailyRes.opmr_invsr_trde?.map {
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
                            openResult = openResult.sortedByDescending { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                        }
                        "2" -> {
                            openResult = openResult.sortedBy { it.netprps_amt?.toLongOrNull() ?: 0L }.stream().limit(100).toList()
                        }
                    }

                    openResult.forEach {
                        investorList.add(
                            InvestorListItem(
                                stkCd = it.stk_cd,
                                stkNm = it.stk_nm,
                                curPrc = it.cur_prc,
                                preSig = it.pre_sig,
                                predPre = it.pred_pre,
                                fluRt = it.flu_rt,
                                accTrdeQty = it.acc_trde_qty,
                                netprpsAmt = it.netprps_amt,
                            )
                        )
                    }
                }
            }
        }

        return InvestorListRes(
            investorList = investorList
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

    private fun isMarketAfter(): Boolean {
        val now = LocalTime.now()

        return now.isAfter(LocalTime.of(15, 30))
    }
}