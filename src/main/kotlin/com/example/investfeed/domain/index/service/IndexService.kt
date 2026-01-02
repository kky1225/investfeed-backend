package com.example.investfeed.domain.index.service

import com.example.investfeed.domain.index.IndexType
import com.example.investfeed.domain.index.dto.req.IndexDetailReq
import com.example.investfeed.domain.index.dto.res.*
import com.example.investfeed.kiwoom.chart.dto.gold.req.GoldChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.req.*
import com.example.investfeed.kiwoom.chart.enum.IndexChartType
import com.example.investfeed.kiwoom.chart.service.SectChartService
import com.example.investfeed.kiwoom.gold.client.GoldClient
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldPriceNowReq
import com.example.investfeed.kiwoom.sect.client.SectClient
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.KiwoomSectPriceNowReq
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList

@Service
class IndexService(
    private val sectClient: SectClient,
    private val sectChartService: SectChartService,
    private val goldClient: GoldClient,
) {
    fun indexList(): IndexListRes? {
        val indexList = IndexType.entries
        val indexListRes: MutableList<IndexListItem> = mutableListOf()

        indexList.forEach { it ->
            val kiwoomSectPriceNowRes = sectClient.sectPriceNow(
                req = KiwoomSectPriceNowReq(
                    mrkt_tp = "0",
                    inds_cd = it.indsCd
                )
            )

            if (kiwoomSectPriceNowRes.return_code == 0) {
                var chartMinuteList: List<ChartMinute> = mutableListOf()

                val kiwoomSectChartMinuteRes = sectChartService.sectChartMinuteList(
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

                indexListRes.add(
                    IndexListItem(
                        indsCd = it.indsCd,
                        indsNm = it.indsNm,
                        curPrc = kiwoomSectPriceNowRes.cur_prc,
                        predPreSig = kiwoomSectPriceNowRes.pred_pre_sig,
                        fluRt = kiwoomSectPriceNowRes.flu_rt,
                        trdeQty = kiwoomSectPriceNowRes.trde_qty,
                        trdePrica = kiwoomSectPriceNowRes.trde_prica,
                        openPric = kiwoomSectPriceNowRes.open_pric,
                        tmN = kiwoomSectPriceNowRes.inds_cur_prc_tm?.first()?.tm_n,
                        chartMinuteList = chartMinuteList
                    )
                )
            }
        }

        return IndexListRes(
            indexList = indexListRes,
            goldPriceRes = goldClient.goldPriceNow(
                req = GoldPriceNowReq(
                    stk_cd = "M04020000"
                )
            ),
            goldChartMinuteListRes = goldClient.goldChartMinuteList(
                req = GoldChartMinuteListReq(
                    stk_cd = "M04020000",
                    tic_scope = "1",
                )
            )
        )
    }

    fun indexDetail(
        req: IndexDetailReq
    ): IndexDetailRes {
        val chartList: MutableList<IndexChart> = mutableListOf()

        when(req.chart_type) {
            IndexChartType.DAY -> {
                val kiwoomSectChartDayRes = sectChartService.sectChartDayList(
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
                sectChartService.sectChartWeekList(
                    req = SectChartWeekListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )
            }
            IndexChartType.MONTH -> {
                sectChartService.sectChartMonthList(
                    req = SectChartMonthListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )
            }
            IndexChartType.YEAR -> {
                sectChartService.sectChartYearList(
                    req = SectChartYearListReq(
                        inds_cd = req.inds_cd,
                        base_dt = today("yyyyMMdd")
                    )
                )
            }
            else -> {
                req.chart_type.value?.let {
                    sectChartService.sectChartMinuteList(
                        req = SectChartMinuteListReq(
                            inds_cd = req.inds_cd,
                            tic_scope = it
                        )
                    )
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
                mrkt_tp = if (req.inds_cd == "101") "1" else "0",
                amt_qty_tp = "0",
                stex_tp = "0"
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
                orgnNetprps = kiwoomSectInvestorRes.inds_netprps?.get(0)?.orgn_netprps
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