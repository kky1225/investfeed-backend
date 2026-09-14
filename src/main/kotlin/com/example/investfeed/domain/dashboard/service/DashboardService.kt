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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Service
class DashboardService(
    private val sectClient: SectClient,
    private val investorClient: InvestorClient,
    private val priceClient: PriceClient,
) {
    private val log = KotlinLogging.logger {}

    fun getStockDashboard(): DashboardRes? = runBlocking {
        log.debug { "dashboard" }

        val rankDeferred = async {
            investorClient.investorTradeRankList(
                req = KiwoomInvestorTradeRankListReq(
                    dt = "20",
                    mrkt_tp = "001",
                    stk_inds_tp = "0",
                    amt_qty_tp = "0",
                    stex_tp = "1"
                )
            )
        }
        val indexList = DashboardIndexType.entries
            .map { indexType -> async { buildIndexItem(indexType) } }
            .awaitAll()
            .filterNotNull()

        val investorTradeRankListRes = rankDeferred.await()
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

        DashboardRes(
            indexList = indexList,
            investorTradeRankList = investorTradeRankListItem,
        )
    }

    private suspend fun buildIndexItem(it: DashboardIndexType): DashboardIndexListItem? = coroutineScope {
        val dailyDeferred = async { sectClient.sectIndexDailyList(req = KiwoomSectIndexDailyReq(mrkt_tp = "0", inds_cd = it.indsCd)) }
        val priceNowDeferred = async { sectClient.sectPriceNow(req = KiwoomSectPriceNowReq(mrkt_tp = "0", inds_cd = it.indsCd)) }
        val investorDeferred = if (it.marketType.isEmpty()) null else async {
            sectClient.sectInvestor(req = KiwoomSectInvestorReq(mrkt_tp = it.marketType, amt_qty_tp = "0", stex_tp = "3"))
        }

        val kiwoomSectIndexDailyRes = dailyDeferred.await()
        val kiwoomSectPriceNowRes = priceNowDeferred.await()
        val kiwoomSectInvestorRes: KiwoomSectInvestorRes? = investorDeferred?.await()

        if (kiwoomSectIndexDailyRes.return_code != 0 || kiwoomSectPriceNowRes.return_code != 0) return@coroutineScope null

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
        }

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
    }
}
