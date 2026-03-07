package com.example.investfeed.domain.index.service

import com.example.investfeed.domain.index.IndexType
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.*
import com.example.investfeed.kiwoom.chart.dto.sect.req.*
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.chart.client.SectChartClient
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomProgramTradeReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList
import kotlin.String

@Service
class IndexService(
    private val sectClient: SectClient,
    private val sectChartClient: SectChartClient,
    private val priceClient: PriceClient
) {
    fun indexList(): IndexListRes? {
        val indexTypeList = IndexType.entries
        val indexList: MutableList<IndexListItem> = mutableListOf()

        indexTypeList.forEach { it ->
            val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = it.indsCd
                )
            )

            if (kiwoomSectPriceNowRes.return_code == 0) {
                var chartMinuteList: List<ChartMinute> = mutableListOf()

                val kiwoomSectChartMinuteRes = sectChartClient.sectChartMinuteList(
                    req = SectChartMinuteListReq(
                        inds_cd = it.indsCd,
                        tic_scope = "1"
                    )
                )

                if (kiwoomSectChartMinuteRes.return_code == 0) {
                    chartMinuteList = kiwoomSectChartMinuteRes.inds_min_pole_qry?.map { it ->
                        ChartMinute(
                            curPrc = it.cur_prc,
                            cntrTm = it.cntr_tm
                        )
                    } ?: emptyList()
                }

                indexList.add(
                    IndexListItem(
                        indsCd = it.indsCd,
                        indsNm = it.indsNm,
                        curPrc = kiwoomSectPriceNowRes.cur_prc,
                        predPreSig = kiwoomSectPriceNowRes.pred_pre_sig,
                        fluRt = kiwoomSectPriceNowRes.flu_rt,
                        trdeQty = kiwoomSectPriceNowRes.trde_qty,
                        trdePrica = kiwoomSectPriceNowRes.trde_prica,
                        openPric = kiwoomSectPriceNowRes.open_pric,
                        tm = kiwoomSectPriceNowRes.inds_cur_prc_tm?.first()?.tm_n,
                        chartMinuteList = chartMinuteList
                    )
                )
            }
        }

        return IndexListRes(
            indexList = indexList
        )
    }

    fun indexDetail(
        req: IndexDetailReq
    ): IndexDetailRes {
        val chartList: MutableList<IndexChart> = mutableListOf()

        when(req.chart_type) {
            IndexChartType.DAY -> {
                val kiwoomSectChartDayRes = sectChartClient.sectChartDayList(
                    req = SectChartDayListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartDayRes.return_code == 0) {
                    kiwoomSectChartDayRes.inds_dt_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.WEEK -> {
                val kiwoomSectChartWeekRes = sectChartClient.sectChartWeekList(
                    req = SectChartWeekListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartWeekRes.return_code == 0) {
                    kiwoomSectChartWeekRes.inds_stk_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.MONTH -> {
                val kiwoomSectChartMonthRes = sectChartClient.sectChartMonthList(
                    req = SectChartMonthListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartMonthRes.return_code == 0) {
                    kiwoomSectChartMonthRes.inds_mth_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            IndexChartType.YEAR -> {
                val kiwoomSectChartYearRes = sectChartClient.sectChartYearList(
                    req = SectChartYearListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )

                if (kiwoomSectChartYearRes.return_code == 0) {
                    kiwoomSectChartYearRes.inds_yr_pole_qry?.forEach {
                        chartList.add(
                            IndexChart(
                                dt = it.dt,
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica
                            )
                        )
                    }
                }
            }
            else -> {
                req.chart_type.value?.let {
                    val kiwoomSectChartMinuteRes = sectChartClient.sectChartMinuteList(
                        req = SectChartMinuteListReq(
                            inds_cd = req.inds_cd,
                            tic_scope = it
                        )
                    )
                    
                    if (kiwoomSectChartMinuteRes.return_code == 0) {
                        kiwoomSectChartMinuteRes.inds_min_pole_qry?.forEach { it ->
                            chartList.add(
                                IndexChart(
                                    dt = it.cntr_tm,
                                    curPrc = it.cur_prc,
                                    openPric = it.open_pric,
                                    highPric = it.high_pric,
                                    lowPric = it.low_pric,
                                    trdeQty = it.trde_qty,
                                    trdePrica = it.acc_trde_qty
                                )
                            )
                        }
                    }
                }
            }
        }

        val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
            req = KiwoomSectPriceNowReq(
                mrkt_tp = "0",
                inds_cd = req.inds_cd
            )
        )

        val kiwoomSectInvestorRes = sectClient.sectInvestor(
            req = KiwoomSectInvestorReq(
                mrkt_tp = if (req.inds_cd == "101" || req.inds_cd == "150") "1" else "0",
                amt_qty_tp = "0",
                stex_tp = "3"
            )
        )

        val kiwoomProgramTradeRes = priceClient.programTrade(
            req = KiwoomProgramTradeReq(
                date = today("yyyyMMdd"),
                amt_qty_tp = "1",
                mrkt_tp = if (req.inds_cd == "001" || req.inds_cd == "201") "P001_AL01" else "P101_AL02",
                min_tic_tp = "1",
                stex_tp = "3",
            )
        )

        return IndexDetailRes(
            indexInfo = IndexInfo(
                indsCd = req.inds_cd,
                indsNm = IndexType.entries.find { it.indsCd == req.inds_cd }?.indsNm,
                curPrc = kiwoomSectPriceNowRes.cur_prc,
                predPreSig = kiwoomSectPriceNowRes.pred_pre_sig,
                predPre = kiwoomSectPriceNowRes.pred_pre,
                fluRt = kiwoomSectPriceNowRes.flu_rt,
                trdeQty = kiwoomSectPriceNowRes.trde_qty,
                trdePrica = kiwoomSectPriceNowRes.trde_prica,
                highPric = kiwoomSectPriceNowRes.high_pric,
                openPric = kiwoomSectPriceNowRes.open_pric,
                lowPric = kiwoomSectPriceNowRes.low_pric,
                _250hgst = kiwoomSectPriceNowRes._52wk_hgst_pric,
                _250lwst = kiwoomSectPriceNowRes._52wk_lwst_pric,
                tmN = kiwoomSectPriceNowRes.inds_cur_prc_tm?.get(0)?.tm_n,
                indNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.ind_netprps,
                frgnrNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.frgnr_netprps,
                orgnNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.orgn_netprps,
                dfrtTrdeNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.dfrt_trde_netprps,
                ndiffproTrdeNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.ndiffpro_trde_netprps,
                allNetprps = kiwoomProgramTradeRes.prm_trde_trnsn?.get(0)?.all_netprps,
            ),
            chartList = chartList
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