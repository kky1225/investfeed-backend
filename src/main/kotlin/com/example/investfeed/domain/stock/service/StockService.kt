package com.example.investfeed.domain.stock.service

import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.domain.stock.dto.req.StockListReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.*
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartDayReq
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartMinuteReq
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartMonthReq
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartWeekReq
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartYearReq
import com.example.investfeed.kiwoom.chart.dto.stock.req.KiwoomStockChartInvestorReq
import com.example.investfeed.kiwoom.chart.enum.StockChartType
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockProgramTradeDayReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockTradeInfoReq
import com.example.investfeed.kiwoom.rank.client.RankClient
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeValueListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomStockTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.KiwoomSurgeTradeVolumeListReq
import com.example.investfeed.kiwoom.chart.client.StockChartClient
import com.example.investfeed.kiwoom.shortselling.client.ShortSellingClient
import com.example.investfeed.kiwoom.shortselling.dto.req.KiwoomStockShortSellingReq
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.String

@Service
class StockService(
    private val stockClient: StockClient,
    private val priceClient: PriceClient,
    private val rankClient: RankClient,
    private val stockChartClient: StockChartClient,
    private val stockSocketClient: StockSocketClient,
    private val shortSellingClient: ShortSellingClient
) {
    private val log = KotlinLogging.logger {}

    fun stockList(
        req: StockListReq
    ): StockListRes {
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
                            stkCd = it.stk_cd,
                            rank = it.now_rank,
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.trde_prica,
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
                            stkCd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.trde_qty,
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
                            stkCd = it.stk_cd,
                            rank = (index + 1).toString(),
                            stkNm = it.stk_nm,
                            fluRt = it.flu_rt,
                            curPrc = it.cur_prc,
                            trdePrica = it.sdnin_rt,
                        )
                    } ?: emptyList()
                )
            }
        }
    }

    fun stockDetail(
        req: StockDetailReq
    ): StockDetailRes {
        val kiwoomStockDefaultInfoRes = stockClient.stockDefaultInfo(
            req = KiwoomDefaultStockInfoReq(
                stk_cd = req.stkCd
            )
        )
        val kiwoomStockInfoRes = stockClient.stockInfo(
            req = KiwoomStockInfoReq(
                stk_cd = req.stkCd.replace("_AL", "").replace("_NXT", "").replace("_SOR", ""),
            )
        )
        val kiwoomStockTradeInfoRes = priceClient.stockTradeInfo(
            req = KiwoomStockTradeInfoReq(
                stk_cd = req.stkCd,
            )
        )
        val kiwoomStockInvestor = stockClient.stockInvestor(
            req = KiwoomStockInvestorReq(
                dt = today("yyyyMMdd"),
                stk_cd = req.stkCd,
                amt_qty_tp = "2",
                trde_tp = "0",
                unit_tp = "1"
            )
        )

        var stockInfo: StockInfo? = null
        if (kiwoomStockDefaultInfoRes.return_code == 0 && kiwoomStockTradeInfoRes.return_code == 0) {
            stockInfo = StockInfo(
                stkCd = kiwoomStockDefaultInfoRes.stk_cd,
                stkNm = kiwoomStockDefaultInfoRes.stk_nm,
                per = kiwoomStockDefaultInfoRes.per,
                eps = kiwoomStockDefaultInfoRes.eps,
                roe = kiwoomStockDefaultInfoRes.roe,
                pbr = kiwoomStockDefaultInfoRes.pbr,
                mac = kiwoomStockDefaultInfoRes.mac,
                macWght = kiwoomStockDefaultInfoRes.mac_wght,
                forExhRt = kiwoomStockDefaultInfoRes.for_exh_rt,
                _250hgst = kiwoomStockDefaultInfoRes._250hgst,
                _250lwst = kiwoomStockDefaultInfoRes._250lwst,
                highPric = kiwoomStockDefaultInfoRes.high_pric,
                openPric = kiwoomStockDefaultInfoRes.open_pric,
                lowPric = kiwoomStockDefaultInfoRes.low_pric,
                curPrc = kiwoomStockDefaultInfoRes.cur_prc,
                preSig = kiwoomStockDefaultInfoRes.pre_sig,
                fluRt = kiwoomStockDefaultInfoRes.flu_rt,
                trdeQty = kiwoomStockDefaultInfoRes.trde_qty,
                trdePrica = kiwoomStockTradeInfoRes.trde_prica,
                tm = kiwoomStockTradeInfoRes.date + time("HHmm"),
                nxtEnable = kiwoomStockInfoRes.nxtEnable,
                orderWarning = kiwoomStockInfoRes.orderWarning,
                marketCode = kiwoomStockInfoRes.marketCode,
                marketName = kiwoomStockInfoRes.marketName,
                upName = kiwoomStockInfoRes.upName
            )
        }

        val stockInvestorList: MutableList<StockInvestor> = mutableListOf()
        if (kiwoomStockInvestor.return_code == 0) {
            kiwoomStockInvestor.stk_invsr_orgn?.forEach {
                stockInvestorList.add(
                    StockInvestor(
                        dt = it.dt,
                        indInvsr = it.ind_invsr,
                        frgnrInvsr = it.frgnr_invsr,
                        orgn = it.orgn,
                        etcFnnc = it.etc_fnnc,
                        fnncInvt = it.fnnc_invt,
                        insrnc = it.insrnc,
                        invtrt = it.invtrt,
                        samoFund = it.samo_fund,
                        penfndEtc = it.penfnd_etc,
                        bank = it.bank,
                        etcCorp = it.etc_corp,
                        natfor = it.natfor
                    )
                )
            }
        }

        val chartListRes: MutableList<StockChart> = mutableListOf()
        when(req.chartType) {
            StockChartType.DAY -> {
                val kiwoomStockChartDayRes = stockChartClient.chartDayList(
                    req = KiwoomStockChartDayReq(
                        stk_cd = req.stkCd,
                        base_dt = today("yyyyMMdd"),
                        upd_stkpc_tp = "1"
                    )
                )

                if (kiwoomStockChartDayRes.return_code == 0) {
                    kiwoomStockChartDayRes.stk_dt_pole_chart_qry?.forEach {
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
                        stk_cd = req.stkCd,
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
                        stk_cd = req.stkCd,
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
                        stk_cd = req.stkCd,
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
                val kiwoomStockChartMinuteRes = req.chartType.value?.let {
                    stockChartClient.chartMinuteList(
                        req = KiwoomStockChartMinuteReq(
                            stk_cd = req.stkCd,
                            tic_scope = it,
                            upd_stkpc_tp = "1"
                        )
                    )
                }

                kiwoomStockChartMinuteRes?.let {
                    if (it.return_code == 0) {
                        kiwoomStockChartMinuteRes.stk_min_pole_chart_qry?.stream()?.filter { kiwoomStockTradeInfoRes.date?.let { date -> it.cntr_tm?.contains(date) == true } == true }?.forEach {
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

        val kiwoomIndexInvestorRes = stockChartClient.stockChartInvestor(
            req = KiwoomStockChartInvestorReq(
                mrkt_tp = "000",
                amt_qty_tp = "2",
                trde_tp = "0",
                stk_cd = req.stkCd,
            )
        )

        val stockInvestorChartList: MutableList<StockInvestorChart> = mutableListOf()
        if (kiwoomIndexInvestorRes.return_code == 0) {
            kiwoomIndexInvestorRes.opmr_invsr_trde_chart?.forEach {
                stockInvestorChartList.add(
                    StockInvestorChart(
                        tm = it.tm,
                        frgnrInvsr = it.frgnr_invsr,
                        orgn = it.orgn,
                        penfnd_etc = it.penfnd_etc,
                    )
                )
            }
        }

        val kiwoomStockProgramTradeDayRes = priceClient.stockProgramTradeDay(
            req = KiwoomStockProgramTradeDayReq(
                amt_qty_tp = "2",
                stk_cd = req.stkCd,
                date = today("yyyyMMdd")
            )
        )

        val stockProgramList: MutableList<StockProgram> = mutableListOf()
        if(kiwoomStockProgramTradeDayRes.return_code == 0) {
            kiwoomStockProgramTradeDayRes.stk_daly_prm_trde_trnsn?.forEach {
                stockProgramList.add(
                    StockProgram(
                        dt = it.dt,
                        prmSellQty = "-" + it.prm_sell_qty,
                        prmBuyQty = it.prm_buy_qty,
                        prmNetprpsQty = it.prm_netprps_qty?.replace("--", "-"),
                        prmNetprpsQtyIrds = it.prm_netprps_qty_irds?.replace("--", "-"),
                    )
                )
            }
        }

        val kiwoomStockShortSellingRes = shortSellingClient.stockShortSelling(
            req = KiwoomStockShortSellingReq(
                stk_cd = req.stkCd,
                tm_tp = "1",
                strt_dt = stockProgramList.lastOrNull()?.dt ?: today("yyyyMMdd"),
                end_dt = stockProgramList.firstOrNull()?.dt ?: today("yyyyMMdd"),
            )
        )

        val stockShortSellingList: MutableList<StockShortSelling> = mutableListOf()
        if (kiwoomStockShortSellingRes.return_code == 0) {
            kiwoomStockShortSellingRes.shrts_trnsn?.forEach {
                stockShortSellingList.add(
                    StockShortSelling(
                        dt = it.dt,
                        trdeQty = it.trde_qty,
                        shrtsQty = it.shrts_qty,
                        trdeWght = it.trde_wght,
                        shrtsTrdePrica = it.shrts_trde_prica,
                        shrtsAvgPric = it.shrts_avg_pric,
                    )
                )
            }
        }

        return StockDetailRes(
            stockInfo = stockInfo,
            stockChartList = chartListRes,
            stockInvestorChartList = stockInvestorChartList,
            stockInvestorList = stockInvestorList,
            stockProgramList = stockProgramList,
            stockShortSellingList = stockShortSellingList
        )
    }

    fun stockSearch(
        keyword: String
    ): List<StockSearchItem> {
        val marketTypes = listOf("0", "10", "8", "60")
        return marketTypes
            .flatMap { mrktTp ->
                try {
                    stockClient.stockInfoList(StockInfoListReq(mrkt_tp = mrktTp))?.list ?: emptyList()
                } catch (e: Exception) {
                    log.warn { "stockSearch mrkt_tp=$mrktTp 조회 실패: ${e.message}" }
                    emptyList()
                }
            }
            .filter { it.name?.contains(keyword, ignoreCase = true) == true }
            .distinctBy { it.code }
            .map { StockSearchItem(stkCd = it.code!! + "_AL", stkNm = it.name!!, marketName = it.marketName!!) }
            .take(20)
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