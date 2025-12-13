package com.example.investfeed.kiwoom.stock.service

import com.example.investfeed.kiwoom.chart.enum.StockChartType
import com.example.investfeed.kiwoom.stock.client.StockChartClient
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.StockListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockStreamReq
import com.example.investfeed.kiwoom.stock.dto.res.*
import com.example.investfeed.kiwoom.stock.entity.req.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class StockService(
    private val stockClient: StockClient,
    private val stockChartClient: StockChartClient,
    private val stockSocketClient: StockSocketClient
) {
    fun stockList(
        req: StockListReq
    ): StockListRes? {
        when (req.type) {
            "0" -> {
                val kiwoomStockTradeValueListRes = stockClient.stockTradeValueList(
                    req = KiwoomStockTradeValueReq(
                        mrkt_tp = "000",
                        mang_stk_incls = "1",
                        stex_tp = "3"
                    )
                )

                return StockListRes(
                    return_code = kiwoomStockTradeValueListRes.return_code,
                    return_msg = kiwoomStockTradeValueListRes.return_msg,
                    stockList = kiwoomStockTradeValueListRes.trde_prica_upper?.map {
                        StockListItemRes(
                            stk_cd = it.stk_cd,
                            rank = it.now_rank,
                            stk_nm = it.stk_nm,
                            flu_rt = it.flu_rt,
                            cur_prc = it.cur_prc,
                            trde_prica = it.trde_prica,
                        )
                    } ?: emptyList()
                )
            }
            "1" -> {
                val kiwoomStockTradeVolumeListRes = stockClient.stockTradeVolumeList(
                    req = KiwoomStockTradeVolumeListReq(
                        mrkt_tp = "000",
                        sort_tp = "1",
                        mang_stk_incls = "0",
                        crd_tp = "0",
                        trde_qty_tp = "0",
                        pric_tp = "0",
                        trde_prica_tp = "0",
                        mrkt_open_tp = "0",
                        stex_tp = "3"
                    )
                )

                return StockListRes(
                    return_code = kiwoomStockTradeVolumeListRes.return_code,
                    return_msg = kiwoomStockTradeVolumeListRes.return_msg,
                    stockList = kiwoomStockTradeVolumeListRes.tdy_trde_qty_upper?.mapIndexed { index, it ->
                        StockListItemRes(
                            stk_cd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stk_nm = it.stk_nm,
                            flu_rt = it.flu_rt,
                            cur_prc = it.cur_prc,
                            trde_prica = it.trde_qty,
                        )
                    } ?: emptyList()
                )
            }
            else -> {
                val kiwoomStockTradeValueListRes = stockClient.stockSurgeTradeVolumeList(
                    req = KiwoomSurgeTradeVolumeListReq(
                        mrkt_tp = "000",
                        sort_tp = "2",
                        tm_tp = "2",
                        trde_qty_tp = "5",
                        stk_cnd = "0",
                        pric_tp = "0",
                        stex_tp = "3"
                    )
                )

                return StockListRes(
                    return_code = kiwoomStockTradeValueListRes.return_code,
                    return_msg = kiwoomStockTradeValueListRes.return_msg,
                    stockList = kiwoomStockTradeValueListRes.trde_qty_sdnin?.mapIndexed { index, it ->
                        StockListItemRes(
                            stk_cd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stk_nm = it.stk_nm,
                            flu_rt = it.flu_rt,
                            cur_prc = it.cur_prc,
                            trde_prica = it.sdnin_rt,
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    fun stockDetail(
        req: KiwoomStockInfoReq
    ): StockDetailRes? {
        val kiwoomStockInfoRes = stockClient.stockInfo(req)
        val kiwoomStockTradeInfoRes = stockClient.stockTradeInfo(
            req = KiwoomStockTradeInfoReq(
                stk_cd = req.stk_cd,
            )
        )
        val kiwoomStockInvestor = stockClient.stockInvestor(
            req = KiwoomStockInvestorReq(
                dt = today("yyyyMMdd"),
                stk_cd = req.stk_cd,
                amt_qty_tp = "2",
                trde_tp = "0",
                unit_tp = "1"
            )
        )

        var stockInfo: StockInfo? = null
        if (kiwoomStockInfoRes.return_code == 0 && kiwoomStockTradeInfoRes.return_code == 0) {
            stockInfo = StockInfo(
                stk_cd = kiwoomStockInfoRes.stk_cd,
                stk_nm = kiwoomStockInfoRes.stk_nm,
                per = kiwoomStockInfoRes.per,
                eps = kiwoomStockInfoRes.eps,
                roe = kiwoomStockInfoRes.roe,
                pbr = kiwoomStockInfoRes.pbr,
                _250hgst = kiwoomStockInfoRes._250hgst,
                _250lwst = kiwoomStockInfoRes._250lwst,
                high_pric = kiwoomStockInfoRes.high_pric,
                open_pric = kiwoomStockInfoRes.open_pric,
                low_pric = kiwoomStockInfoRes.low_pric,
                cur_prc = kiwoomStockInfoRes.cur_prc,
                pre_sig = kiwoomStockInfoRes.pre_sig,
                flu_rt = kiwoomStockInfoRes.flu_rt,
                trde_qty = kiwoomStockInfoRes.trde_qty,
                trde_prica = kiwoomStockTradeInfoRes.trde_prica,
                tm = kiwoomStockTradeInfoRes.date + time("HHmm"),
            )
        }

        val stockInvestorList: MutableList<StockInvestor> = mutableListOf()
        if (kiwoomStockInvestor.return_code == 0) {
            kiwoomStockInvestor.stk_invsr_orgn?.forEach {
                stockInvestorList.add(
                    StockInvestor(
                        dt = it.dt,
                        ind_invsr = it.ind_invsr,
                        frgnr_invsr = it.frgnr_invsr,
                        orgn = it.orgn,
                        etc_fnnc = it.etc_fnnc,
                        fnnc_invt = it.fnnc_invt,
                        insrnc = it.insrnc,
                        invtrt = it.invtrt,
                        samo_fund = it.samo_fund,
                        penfnd_etc = it.penfnd_etc,
                        bank = it.bank,
                        etc_corp = it.etc_corp,
                        natfor = it.natfor,
                    )
                )
            }
        }

        val chartListRes: MutableList<StockChart> = mutableListOf()
        when(req.chart_type) {
            StockChartType.DAY -> {
                val kiwoomStockChartDayRes = stockChartClient.chartDayList(
                    req = KiwoomStockChartDayReq(
                        stk_cd = req.stk_cd,
                        base_dt = today("yyyyMMdd"),
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomStockChartDayRes.return_code == 0) {
                    kiwoomStockChartDayRes.stk_dt_pole_chart_qry?.stream()?.forEach {
                        chartListRes.add(
                            StockChart(
                                dt = it.dt,
                                cur_prc = it.cur_prc,
                                open_pric = it.open_pric,
                                high_pric = it.high_pric,
                                low_pric = it.low_pric,
                                trde_qty = it.trde_qty,
                                trde_prica = it.trde_prica,
                            )
                        )
                    }
                }
            }
            StockChartType.WEEK -> {
                val kiwoomStockChartWeekRes = stockChartClient.chartWeekList(
                    req = KiwoomStockChartWeekReq(
                        stk_cd = req.stk_cd,
                        base_dt = today("yyyyMMdd"),
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomStockChartWeekRes.return_code == 0) {
                    kiwoomStockChartWeekRes.stk_stk_pole_chart_qry?.stream()?.forEach {
                        chartListRes.add(
                            StockChart(
                                dt = it.dt,
                                cur_prc = it.cur_prc,
                                open_pric = it.open_pric,
                                high_pric = it.high_pric,
                                low_pric = it.low_pric,
                                trde_qty = it.trde_qty,
                                trde_prica = it.trde_prica,
                            )
                        )
                    }
                }
            }
            StockChartType.MONTH -> {
                val kiwoomStockChartMonthRes = stockChartClient.chartMonthList(
                    req = KiwoomStockChartMonthReq(
                        stk_cd = req.stk_cd,
                        base_dt = today("yyyyMMdd"),
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomStockChartMonthRes.return_code == 0) {
                    kiwoomStockChartMonthRes.stk_mth_pole_chart_qry?.stream()?.forEach {
                        chartListRes.add(
                            StockChart(
                                dt = it.dt,
                                cur_prc = it.cur_prc,
                                open_pric = it.open_pric,
                                high_pric = it.high_pric,
                                low_pric = it.low_pric,
                                trde_qty = it.trde_qty,
                                trde_prica = it.trde_prica,
                            )
                        )
                    }
                }
            }
            StockChartType.YEAR -> {
                val kiwoomStockChartYearRes = stockChartClient.chartYearList(
                    req = KiwoomStockChartYearReq(
                        stk_cd = req.stk_cd,
                        base_dt = today("yyyyMMdd"),
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomStockChartYearRes.return_code == 0) {
                    kiwoomStockChartYearRes.stk_yr_pole_chart_qry?.stream()?.forEach {
                        chartListRes.add(
                            StockChart(
                                dt = it.dt,
                                cur_prc = it.cur_prc,
                                open_pric = it.open_pric,
                                high_pric = it.high_pric,
                                low_pric = it.low_pric,
                                trde_qty = it.trde_qty,
                                trde_prica = it.trde_prica,
                            )
                        )
                    }
                }
            }
            else -> {
                val kiwoomStockChartMinuteRes = req.chart_type.value?.let {
                    stockChartClient.chartMinuteList(
                        req = KiwoomStockChartMinuteReq(
                            stk_cd = req.stk_cd,
                            tic_scope = it,
                            upd_stkpc_tp = "1"
                        )
                    )
                }

                kiwoomStockChartMinuteRes?.let {
                    if (it.return_code == 0) {
                        kiwoomStockChartMinuteRes.stk_min_pole_chart_qry?.stream()?.filter { it.cntr_tm?.contains(today("yyyyMMdd")) == true }?.forEach {
                            chartListRes.add(
                                StockChart(
                                    dt = it.cntr_tm,
                                    cur_prc = it.cur_prc,
                                    open_pric = it.open_pric,
                                    high_pric = it.high_pric,
                                    low_pric = it.low_pric,
                                    trde_qty = it.trde_qty,
                                )
                            )
                        }
                    }
                }
            }
        }

        return StockDetailRes(
            stockInfo = stockInfo,
            stockChartList = chartListRes,
            stockInvestorList = stockInvestorList
        )
    }

    fun stockStream(
        req: StockStreamReq
    ) {
        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0A")
                    )
                )
            )
        )
    }

    fun today(
        pattern: String
    ): String {
        val now = LocalDate.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }

    fun time(
        pattern: String
    ): String {
        val now = LocalTime.now()
        val pattern = DateTimeFormatter.ofPattern(pattern)

        return pattern.format(now)
    }
}