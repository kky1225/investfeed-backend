package com.example.investfeed.domain.stock.service

import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockListReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.*
import com.example.investfeed.kiwoom.chart.enum.StockChartType
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockTradeInfoReq
import com.example.investfeed.kiwoom.rank.client.RankClient
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeValueListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomSurgeTradeVolumeListReq
import com.example.investfeed.kiwoom.stock.client.StockChartClient
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.*
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class StockService(
    private val stockClient: StockClient,
    private val priceClient: PriceClient,
    private val rankClient: RankClient,
    private val stockChartClient: StockChartClient,
    private val stockSocketClient: StockSocketClient
) {
    fun stockList(
        req: StockListReq
    ): StockListRes? {
        when (req.type) {
            "0" -> {
                val kiwoomStockTradeValueListRes = rankClient.stockTradeValueList(
                    req = KiwoomStockTradeValueListReq(
                        mrkt_tp = "000",
                        mang_stk_incls = "1",
                        stex_tp = "3"
                    )
                )

                return StockListRes(
                    return_code = kiwoomStockTradeValueListRes.return_code,
                    return_msg = kiwoomStockTradeValueListRes.return_msg,
                    stockList = kiwoomStockTradeValueListRes.trde_prica_upper?.map {
                        StockListItem(
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
                val kiwoomStockTradeVolumeListRes = rankClient.stockTradeVolumeList(
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
                        StockListItem(
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
                val kiwoomStockTradeValueListRes = rankClient.stockSurgeTradeVolumeList(
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
                        StockListItem(
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
        req: StockDetailReq
    ): StockDetailRes? {
        val kiwoomStockDefaultInfoRes = stockClient.stockDefaultInfo(
            req = KiwoomDefaultStockInfoReq(
                stk_cd = req.stk_cd
            )
        )
        val kiwoomStockInfoRes = stockClient.stockInfo(
            req = KiwoomStockInfoReq(
                stk_cd = req.stk_cd.replace("_AL", "").replace("_NXT", "").replace("_SOR", ""),
            )
        )
        val kiwoomStockTradeInfoRes = priceClient.stockTradeInfo(
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
        if (kiwoomStockDefaultInfoRes.return_code == 0 && kiwoomStockTradeInfoRes.return_code == 0) {
            stockInfo = StockInfo(
                stk_cd = kiwoomStockDefaultInfoRes.stk_cd,
                stk_nm = kiwoomStockDefaultInfoRes.stk_nm,
                per = kiwoomStockDefaultInfoRes.per,
                eps = kiwoomStockDefaultInfoRes.eps,
                roe = kiwoomStockDefaultInfoRes.roe,
                pbr = kiwoomStockDefaultInfoRes.pbr,
                _250hgst = kiwoomStockDefaultInfoRes._250hgst,
                _250lwst = kiwoomStockDefaultInfoRes._250lwst,
                high_pric = kiwoomStockDefaultInfoRes.high_pric,
                open_pric = kiwoomStockDefaultInfoRes.open_pric,
                low_pric = kiwoomStockDefaultInfoRes.low_pric,
                cur_prc = kiwoomStockDefaultInfoRes.cur_prc,
                pre_sig = kiwoomStockDefaultInfoRes.pre_sig,
                flu_rt = kiwoomStockDefaultInfoRes.flu_rt,
                trde_qty = kiwoomStockDefaultInfoRes.trde_qty,
                trde_prica = kiwoomStockTradeInfoRes.trde_prica,
                tm = kiwoomStockTradeInfoRes.date + time("HHmm"),
                nxtEnable = kiwoomStockInfoRes.nxtEnable,
                orderWarning = kiwoomStockInfoRes.orderWarning,
                marketCode = kiwoomStockInfoRes.marketCode,
                marketName = kiwoomStockInfoRes.marketName,
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
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica,
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
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica,
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
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica,
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
                                curPrc = it.cur_prc,
                                openPric = it.open_pric,
                                highPric = it.high_pric,
                                lowPric = it.low_pric,
                                trdeQty = it.trde_qty,
                                trdePrica = it.trde_prica,
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
                                    curPrc = it.cur_prc,
                                    openPric = it.open_pric,
                                    highPric = it.high_pric,
                                    lowPric = it.low_pric,
                                    trdeQty = it.trde_qty,
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
                        type = listOf("0B")
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