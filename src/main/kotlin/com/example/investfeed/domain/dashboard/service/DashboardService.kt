package com.example.investfeed.domain.dashboard.service

import com.example.investfeed.domain.dashboard.DashboardIndexType
import com.example.investfeed.domain.dashboard.dto.res.ChartDay
import com.example.investfeed.domain.dashboard.dto.res.DashboardIndexListItem
import com.example.investfeed.domain.dashboard.dto.res.DashboardRes
import com.example.investfeed.domain.dashboard.dto.res.InvestorTradeRankListItem
import com.example.investfeed.kiwoom.investor.client.InvestorClient
import com.example.investfeed.kiwoom.investor.dto.req.KiwoomInvestorTradeRankListReq
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomProgramTradeReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectIndexDailyReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectInvestorRes
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val sectClient: SectClient,
    private val investorClient: InvestorClient,
    private val priceClient: PriceClient,
) {
    private val log = KotlinLogging.logger {}

    fun dashboard(): DashboardRes? {
        log.debug { "dashboard" }

        val indexTypeList = DashboardIndexType.entries
        val indexList: MutableList<DashboardIndexListItem> = mutableListOf()

        indexTypeList.forEach {
            val kiwoomSectIndexDailyRes = sectClient.sectIndexDailyList(
                req = KiwoomSectIndexDailyReq(
                    mrkt_tp = "0",
                    inds_cd = it.indsCd
                )
            )

            val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = it.indsCd
                )
            )

            var kiwoomSectInvestorRes: KiwoomSectInvestorRes? = null

            if (!it.marketType.isEmpty()) {
                kiwoomSectInvestorRes = sectClient.sectInvestor(
                    req = KiwoomSectInvestorReq(
                        mrkt_tp = it.marketType,
                        amt_qty_tp = "0",
                        stex_tp = "3"
                    )
                );
            }

            if (kiwoomSectIndexDailyRes.return_code == 0 && kiwoomSectPriceNowRes.return_code == 0) {
                val chartList: MutableList<ChartDay> = mutableListOf()

                kiwoomSectIndexDailyRes.inds_cur_prc_daly_rept?.forEach {
                    chartList.add(
                        ChartDay(
                            curPrc = it.cur_prc_n,
                            dt = it.dt_n,
                        )
                    )
                }

                var dfrtTrdeNetprps: String? = null
                var ndiffproTrdeNetprps: String? = null
                var allNetprps: String? = null

                if (it.marketType.isNotEmpty()) {
                    try {
                        val mrktTp = if (it.indsCd == "001") "P001_AL01" else "P101_AL02"
                        val programTradeRes = priceClient.programTrade(
                            req = KiwoomProgramTradeReq(
                                date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                                amt_qty_tp = "1",
                                mrkt_tp = mrktTp,
                                min_tic_tp = "1",
                                stex_tp = "3",
                            )
                        )

                        if (programTradeRes.return_code == 0) {
                            programTradeRes.prm_trde_trnsn?.firstOrNull()?.let { trade ->
                                dfrtTrdeNetprps = trade.dfrt_trde_netprps
                                ndiffproTrdeNetprps = trade.ndiffpro_trde_netprps
                                allNetprps = trade.all_netprps
                            }
                        }
                    } catch (e: Exception) {
                        log.error { "대시보드 프로그램매매 조회 실패 (${it.indsNm}): ${e.message}" }
                    }
                }

                indexList.add(
                    DashboardIndexListItem(
                        indsCd = it.indsCd,
                        indsNm = it.indsNm,
                        curPrc = kiwoomSectIndexDailyRes.cur_prc,
                        predPreSig = kiwoomSectIndexDailyRes.pred_pre_sig,
                        fluRt = kiwoomSectIndexDailyRes.flu_rt,
                        tm = kiwoomSectIndexDailyRes.inds_cur_prc_daly_rept?.get(0)?.dt_n + kiwoomSectPriceNowRes.inds_cur_prc_tm?.get(0)?.tm_n,
                        ind = kiwoomSectInvestorRes?.inds_netprps?.get(0)?.ind_netprps,
                        orgn = kiwoomSectInvestorRes?.inds_netprps?.get(0)?.orgn_netprps,
                        frgnr = kiwoomSectInvestorRes?.inds_netprps?.get(0)?.frgnr_netprps,
                        chartList = chartList,
                        dfrtTrdeNetprps = dfrtTrdeNetprps,
                        ndiffproTrdeNetprps = ndiffproTrdeNetprps,
                        allNetprps = allNetprps,
                    )
                )
            }
        }

        val investorTradeRankListRes = investorClient.investorTradeRankList(
            req = KiwoomInvestorTradeRankListReq(
                dt = "20",
                mrkt_tp = "001",
                stk_inds_tp = "0",
                amt_qty_tp = "0",
                stex_tp = "1"
            )
        )

        val investorTradeRankListItem: MutableList<InvestorTradeRankListItem> = mutableListOf()
        if (investorTradeRankListRes.return_code == 0) {
            investorTradeRankListRes.orgn_frgnr_cont_trde_prst?.forEach {
                investorTradeRankListItem.add(
                    InvestorTradeRankListItem(
                        stkCd = it.stk_cd,
                        rank = it.rank,
                        stkNm = it.stk_nm,
                        pridStkpcFluRt = it.prid_stkpc_flu_rt,
                        nettrdeAmt = it.nettrde_amt
                    )
                )
            }
        }

        return DashboardRes(
            indexList = indexList,
            investorTradeRankList = investorTradeRankListItem,
        )
    }
}
