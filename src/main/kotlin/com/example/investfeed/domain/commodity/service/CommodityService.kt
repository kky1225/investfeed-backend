package com.example.investfeed.domain.commodity.service

import com.example.investfeed.domain.commodity.CommodityType
import com.example.investfeed.domain.commodity.dto.req.CommodityDetailReq
import com.example.investfeed.domain.commodity.dto.res.*
import com.example.investfeed.domain.index.dto.res.ChartMinute
import com.example.investfeed.kiwoom.chart.client.GoldChartClient
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartDayReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMinuteReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartMonthReq
import com.example.investfeed.kiwoom.chart.dto.gold.req.KiwoomGoldChartWeekReq
import com.example.investfeed.kiwoom.chart.enum.CommodityChartType
import com.example.investfeed.kiwoom.investor.client.InvestorClient
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomGoldPriceNowReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockTradeInfoReq
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomDefaultStockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockInfoReq
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class CommodityService(
    private val priceClient: PriceClient,
    private val goldChartClient: GoldChartClient,
    private val investorClient: InvestorClient,
    private val stockClient: StockClient,
) {
    fun commodityList(): CommodityListRes {
        val commodityTypeList = CommodityType.entries
        val commodityList: MutableList<CommodityListItem> = mutableListOf()

        commodityTypeList.forEach { it ->
            val kiwoomGoldPriceNowRes = priceClient.goldPriceNow(
                req = KiwoomGoldPriceNowReq(
                    stk_cd = it.stkCd
                )
            )

            if (kiwoomGoldPriceNowRes.return_code == 0) {
                var chartMinuteList: List<ChartMinute> = mutableListOf()
                val kiwoomGoldChartMinuteRes = goldChartClient.goldChartMinuteList(
                    req = KiwoomGoldChartMinuteReq(
                        stk_cd = it.stkCd,
                        tic_scope = "1"
                    )
                )

                if (kiwoomGoldChartMinuteRes.return_code == 0) {
                    chartMinuteList = kiwoomGoldChartMinuteRes.gds_min_chart_qry?.map {
                        ChartMinute(
                            curPrc = it.cntr_pric,
                            cntrTm = it.cntr_tm
                        )
                    } ?: emptyList()
                }

                val curPrc = kiwoomGoldPriceNowRes.pred_pre?.toInt()?.let { pred_pre -> kiwoomGoldPriceNowRes.pred_close_pric?.toInt()?.plus(pred_pre) }

                commodityList.add(
                    CommodityListItem(
                        stkCd = it.stkCd,
                        stkNm = it.stkNm,
                        curPrc = curPrc.toString(),
                        predPreSig = kiwoomGoldPriceNowRes.pred_pre_sig,
                        predPre = kiwoomGoldPriceNowRes.pred_pre,
                        fluRt = kiwoomGoldPriceNowRes.flu_rt,
                        trdeQty = kiwoomGoldPriceNowRes.trde_qty,
                        openPric = kiwoomGoldPriceNowRes.open_pric,
                        tm = kiwoomGoldChartMinuteRes.gds_min_chart_qry?.first()?.cntr_tm,
                        chartMinuteList = chartMinuteList
                    )
                )
            }
        }

        return CommodityListRes(
            commodityList = commodityList
        )
    }

    fun commodityDetail(
        req: CommodityDetailReq
    ): CommodityDetailRes {
        val kiwoomStockDefaultInfoRes = stockClient.stockDefaultInfo(
            req = KiwoomDefaultStockInfoReq(
                stk_cd = req.stkCd
            )
        )
        val kiwoomStockInfoRes = stockClient.stockInfo(
            req = KiwoomStockInfoReq(
                stk_cd = req.stkCd
            )
        )
        val kiwoomStockTradeInfoRes = priceClient.stockTradeInfo(
            req = KiwoomStockTradeInfoReq(
                stk_cd = req.stkCd,
            )
        )
        val kiwoomGoldInvestorRes = investorClient.goldInvestor()

        var commodityInfo: CommodityInfo? = null
        if (kiwoomStockDefaultInfoRes.return_code == 0 && kiwoomStockTradeInfoRes.return_code == 0 && kiwoomGoldInvestorRes.return_code == 0) {
            commodityInfo = CommodityInfo(
                stkCd = req.stkCd,
                stkNm = CommodityType.entries.find { it.stkCd == req.stkCd }?.stkNm,
                curPrc = kiwoomStockDefaultInfoRes.cur_prc,
                predPreSig = kiwoomStockDefaultInfoRes.pre_sig,
                predPre = kiwoomStockDefaultInfoRes.pred_pre,
                fluRt = kiwoomStockDefaultInfoRes.flu_rt,
                trdeQty = kiwoomStockDefaultInfoRes.trde_qty,
                trdePrica = kiwoomStockTradeInfoRes.trde_prica,
                highPric = kiwoomStockDefaultInfoRes.high_pric,
                openPric = kiwoomStockDefaultInfoRes.open_pric,
                lowPric = kiwoomStockDefaultInfoRes.low_pric,
                tm = kiwoomStockTradeInfoRes.date + time("HHmm"),
                _250hgst = kiwoomStockDefaultInfoRes._250hgst,
                _250lwst = kiwoomStockDefaultInfoRes._250lwst,
                indNetprps = kiwoomGoldInvestorRes.inve_trad_stat?.get(0)?.all_dfrt_trst_netprps_amt,
                frgnrNetprps = kiwoomGoldInvestorRes.inve_trad_stat?.get(2)?.all_dfrt_trst_netprps_amt,
                orgnNetprps = kiwoomGoldInvestorRes.inve_trad_stat?.get(1)?.all_dfrt_trst_netprps_amt,
                nxtEnable = kiwoomStockInfoRes.nxtEnable,
                orderWarning = kiwoomStockInfoRes.orderWarning,
                marketCode = kiwoomStockInfoRes.marketCode,
                marketName = kiwoomStockInfoRes.marketName,
            )
        }

        val chartListRes: MutableList<CommodityChart> = mutableListOf()
        val baseDt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        when(req.chartType) {
            CommodityChartType.DAY -> {
                val kiwoomGoldChartDayRes = goldChartClient.goldChartDayList(
                    req = KiwoomGoldChartDayReq(
                        stk_cd = req.stkCd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomGoldChartDayRes.return_code == 0) {
                    kiwoomGoldChartDayRes.gds_day_chart_qry?.forEach { it ->
                        chartListRes.add(
                            CommodityChart(
                                curPrc = it.cur_prc,
                                trdeQty = it.acc_trde_qty,
                                dt = it.dt,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdePrica = it.acc_trde_prica,
                            )
                        )
                    }
                }
            }
            CommodityChartType.WEEK -> {
                val kiwoomGoldChartWeekRes = goldChartClient.goldChartWeekList(
                    req = KiwoomGoldChartWeekReq(
                        stk_cd = req.stkCd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomGoldChartWeekRes.return_code == 0) {
                    kiwoomGoldChartWeekRes.gds_week_chart_qry?.forEach {
                        chartListRes.add(
                            CommodityChart(
                                curPrc = it.cur_prc,
                                trdeQty = it.acc_trde_qty,
                                dt = it.dt,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdePrica = it.acc_trde_prica,
                            )
                        )
                    }
                }
            }
            CommodityChartType.MONTH -> {
                val kiwoomGoldChartMonthRes = goldChartClient.goldChartMonthList(
                    req = KiwoomGoldChartMonthReq(
                        stk_cd = req.stkCd,
                        base_dt = baseDt,
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomGoldChartMonthRes.return_code == 0) {
                    kiwoomGoldChartMonthRes.gds_month_chart_qry?.forEach {
                        chartListRes.add(
                            CommodityChart(
                                curPrc = it.cur_prc,
                                trdeQty = it.acc_trde_qty,
                                dt = it.dt,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdePrica = it.acc_trde_prica,
                            )
                        )
                    }
                }
            }
            else -> {
                val kiwoomGoldChartMinuteRes = req.chartType.value?.let {
                    goldChartClient.goldChartMinuteList(
                        req = KiwoomGoldChartMinuteReq(
                            stk_cd = req.stkCd,
                            tic_scope = it,
                            upd_stkpc_tp = "1"
                        )
                    )
                }

                if (kiwoomGoldChartMinuteRes?.return_code == 0) {
                    kiwoomGoldChartMinuteRes.gds_min_chart_qry?.forEach {
                        chartListRes.add(
                            CommodityChart(
                                curPrc = it.cntr_pric,
                                trdeQty = it.acc_trde_qty,
                                dt = it.dt,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdePrica = it.acc_trde_prica,
                            )
                        )
                    }
                }
            }
        }

        return CommodityDetailRes(
            commodityInfo = commodityInfo,
            commodityChartList = chartListRes
        )
    }

    fun time(
        pattern: String
    ): String {
        val now = LocalTime.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}