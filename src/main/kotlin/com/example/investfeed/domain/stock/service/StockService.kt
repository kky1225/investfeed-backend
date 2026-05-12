package com.example.investfeed.domain.stock.service

import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockInfoListReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.*
import com.example.investfeed.kiwoom.chart.client.StockChartClient
import com.example.investfeed.kiwoom.chart.dto.stock.req.*
import com.example.investfeed.kiwoom.chart.enum.StockChartType
import com.example.investfeed.common.util.DateUtil
import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.kiwoom.price.client.PriceClient
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockProgramTradeDayReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockProgramTradeMinuteReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockSinglePriceReq
import com.example.investfeed.kiwoom.price.dto.req.KiwoomStockTradeInfoReq
import com.example.investfeed.kiwoom.shortselling.client.ShortSellingClient
import com.example.investfeed.kiwoom.shortselling.dto.req.KiwoomStockShortSellingReq
import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.stock.dto.req.*
import com.example.investfeed.domain.dividend.service.StockDividendService
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class StockService(
    private val stockClient: StockClient,
    private val priceClient: PriceClient,
    private val stockChartClient: StockChartClient,
    private val stockSocketClient: StockSocketClient,
    private val shortSellingClient: ShortSellingClient,
    private val stockDividendService: StockDividendService,
) {
    private val log = KotlinLogging.logger {}

    fun getStock(
        stkCd: String,
        req: StockDetailReq
    ): StockDetailRes {
        val kiwoomStockDefaultInfoRes = stockClient.stockDefaultInfo(
            req = KiwoomDefaultStockInfoReq(
                stk_cd = stkCd
            )
        )
        val kiwoomStockInfoRes = stockClient.stockInfo(
            req = KiwoomStockInfoReq(
                stk_cd = stkCd.replace("_AL", "").replace("_NXT", "").replace("_SOR", ""),
            )
        )
        val kiwoomStockTradeInfoRes = priceClient.stockTradeInfo(
            req = KiwoomStockTradeInfoReq(
                stk_cd = stkCd,
            )
        )
        val kiwoomStockInvestor = stockClient.stockInvestor(
            req = KiwoomStockInvestorReq(
                dt = DateUtil.today("yyyyMMdd"),
                stk_cd = stkCd,
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
                predPre = kiwoomStockDefaultInfoRes.pred_pre,
                fluRt = kiwoomStockDefaultInfoRes.flu_rt,
                trdeQty = kiwoomStockDefaultInfoRes.trde_qty,
                trdePrica = kiwoomStockTradeInfoRes.trde_prica,
                tm = kiwoomStockTradeInfoRes.date + DateUtil.time("HHmm"),
                nxtEnable = kiwoomStockInfoRes.nxtEnable,
                orderWarning = kiwoomStockInfoRes.orderWarning,
                auditInfo = kiwoomStockInfoRes.auditInfo,
                state = kiwoomStockInfoRes.state,
                marketCode = kiwoomStockInfoRes.marketCode,
                marketName = kiwoomStockInfoRes.marketName,
                upName = kiwoomStockInfoRes.upName,
            ).apply {
                when {
                    MarketTimeUtil.isOvtSinglePrice() && marketName == "ETF" -> {
                        val ovtRes = priceClient.stockSinglePriceList(
                            req = KiwoomStockSinglePriceReq(
                                stk_cd = stkCd.replace("_AL", "").replace("_NXT", "").replace("_SOR", "")
                            )
                        )
                        ovtRes?.let {
                            val pric = it.ovt_sigpric_cur_prc
                            if (!pric.isNullOrBlank() && pric != "0") {
                                expCntrPric = pric
                                expCntrFluRt = it.ovt_sigpric_flu_rt
                                expCntrPreSig = it.ovt_sigpric_pred_pre_sig
                            }
                        }
                    }
                    MarketTimeUtil.isCallAuction() -> {
                        val pric = kiwoomStockDefaultInfoRes.exp_cntr_pric
                        if (!pric.isNullOrBlank() && pric != "0") {
                            expCntrPric = pric
                        }
                    }
                }
            }
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
                        natn = it.natn,
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
                        stk_cd = stkCd,
                        base_dt = DateUtil.today("yyyyMMdd"),
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
                        stk_cd = stkCd,
                        base_dt = DateUtil.today("yyyyMMdd"),
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
                        stk_cd = stkCd,
                        base_dt = DateUtil.today("yyyyMMdd"),
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
                        stk_cd = stkCd,
                        base_dt = DateUtil.today("yyyyMMdd"),
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
                            stk_cd = stkCd,
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
                stk_cd = stkCd,
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
                stk_cd = stkCd,
                date = DateUtil.today("yyyyMMdd")
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
                stk_cd = stkCd,
                tm_tp = "1",
                strt_dt = stockProgramList.lastOrNull()?.dt ?: DateUtil.today("yyyyMMdd"),
                end_dt = stockProgramList.firstOrNull()?.dt ?: DateUtil.today("yyyyMMdd"),
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

        val dividendList = stockDividendService.getDividendList(stkCd, kiwoomStockInfoRes.marketCode)

        return StockDetailRes(
            stockInfo = stockInfo,
            stockChartList = chartListRes,
            stockInvestorChartList = stockInvestorChartList,
            stockInvestorList = stockInvestorList,
            stockProgramList = stockProgramList,
            stockShortSellingList = stockShortSellingList,
            dividendList = dividendList,
        )
    }


    fun getStockChart(stkCd: String, req: StockDetailReq): StockChartRes {
        val kiwoomStockDefaultInfoRes = stockClient.stockDefaultInfo(
            req = KiwoomDefaultStockInfoReq(stk_cd = stkCd)
        )
        val kiwoomStockInfoRes = stockClient.stockInfo(
            req = KiwoomStockInfoReq(stk_cd = stkCd.replace("_AL", "").replace("_NXT", "").replace("_SOR", ""))
        )
        val kiwoomStockTradeInfoRes = priceClient.stockTradeInfo(
            req = KiwoomStockTradeInfoReq(stk_cd = stkCd)
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
                predPre = kiwoomStockDefaultInfoRes.pred_pre,
                fluRt = kiwoomStockDefaultInfoRes.flu_rt,
                trdeQty = kiwoomStockDefaultInfoRes.trde_qty,
                trdePrica = kiwoomStockTradeInfoRes.trde_prica,
                tm = kiwoomStockTradeInfoRes.date + DateUtil.time("HHmm"),
                nxtEnable = kiwoomStockInfoRes.nxtEnable,
                orderWarning = kiwoomStockInfoRes.orderWarning,
                auditInfo = kiwoomStockInfoRes.auditInfo,
                state = kiwoomStockInfoRes.state,
                marketCode = kiwoomStockInfoRes.marketCode,
                marketName = kiwoomStockInfoRes.marketName,
                upName = kiwoomStockInfoRes.upName,
            )
        }

        val chartListRes: MutableList<StockChart> = mutableListOf()
        when (req.chartType) {
            StockChartType.DAY -> {
                val res = stockChartClient.chartDayList(KiwoomStockChartDayReq(stk_cd = stkCd, base_dt = DateUtil.today("yyyyMMdd"), upd_stkpc_tp = "1"))
                if (res.return_code == 0) res.stk_dt_pole_chart_qry?.forEach { chartListRes.add(StockChart(dt = it.dt, curPrc = it.cur_prc, openPric = it.open_pric, highPric = it.high_pric, lowPric = it.low_pric, trdeQty = it.trde_qty, trdePrica = it.trde_prica)) }
            }
            StockChartType.WEEK -> {
                val res = stockChartClient.chartWeekList(KiwoomStockChartWeekReq(stk_cd = stkCd, base_dt = DateUtil.today("yyyyMMdd"), upd_stkpc_tp = "1"))
                if (res.return_code == 0) res.stk_stk_pole_chart_qry?.forEach { chartListRes.add(StockChart(dt = it.dt, curPrc = it.cur_prc, openPric = it.open_pric, highPric = it.high_pric, lowPric = it.low_pric, trdeQty = it.trde_qty, trdePrica = it.trde_prica)) }
            }
            StockChartType.MONTH -> {
                val res = stockChartClient.chartMonthList(KiwoomStockChartMonthReq(stk_cd = stkCd, base_dt = DateUtil.today("yyyyMMdd"), upd_stkpc_tp = "1"))
                if (res.return_code == 0) res.stk_mth_pole_chart_qry?.forEach { chartListRes.add(StockChart(dt = it.dt, curPrc = it.cur_prc, openPric = it.open_pric, highPric = it.high_pric, lowPric = it.low_pric, trdeQty = it.trde_qty, trdePrica = it.trde_prica)) }
            }
            StockChartType.YEAR -> {
                val res = stockChartClient.chartYearList(KiwoomStockChartYearReq(stk_cd = stkCd, base_dt = DateUtil.today("yyyyMMdd"), upd_stkpc_tp = "1"))
                if (res.return_code == 0) res.stk_yr_pole_chart_qry?.forEach { chartListRes.add(StockChart(dt = it.dt, curPrc = it.cur_prc, openPric = it.open_pric, highPric = it.high_pric, lowPric = it.low_pric, trdeQty = it.trde_qty, trdePrica = it.trde_prica)) }
            }
            else -> {
                req.chartType.value?.let { tic ->
                    val res = stockChartClient.chartMinuteList(KiwoomStockChartMinuteReq(stk_cd = stkCd, tic_scope = tic, upd_stkpc_tp = "1"))
                    if (res.return_code == 0) res.stk_min_pole_chart_qry?.filter { kiwoomStockTradeInfoRes.date?.let { date -> it.cntr_tm?.contains(date) == true } == true }?.forEach {
                        chartListRes.add(StockChart(dt = it.cntr_tm, curPrc = it.cur_prc, openPric = it.open_pric, highPric = it.high_pric, lowPric = it.low_pric, trdeQty = it.trde_qty))
                    }
                }
            }
        }

        return StockChartRes(stockInfo = stockInfo, stockChartList = chartListRes)
    }

    fun getStockProgramChart(
        stkCd: String
    ): List<StockProgramChart> {
        val stockProgramChartList: MutableList<StockProgramChart> = mutableListOf()

        val kiwoomStockProgramTradeMinuteRes = priceClient.stockProgramTradeMinute(
            req = KiwoomStockProgramTradeMinuteReq(
                amt_qty_tp = "2",
                stk_cd = stkCd,
                date = DateUtil.today("yyyyMMdd")
            )
        )

        if (kiwoomStockProgramTradeMinuteRes.return_code == 0) {
            kiwoomStockProgramTradeMinuteRes.stk_tm_prm_trde_trnsn
                ?.groupBy { it.tm?.substring(0, 4) }
                ?.map { (_, items) -> items.last() }
                ?.forEach {
                    stockProgramChartList.add(
                        StockProgramChart(
                            tm = it.tm?.substring(0, 4),
                            prmSellAmt = it.prm_sell_qty,
                            prmBuyAmt = it.prm_buy_qty,
                            prmNetprpsAmt = it.prm_netprps_qty?.replace("--", "-"),
                        )
                    )
                }
        }

        return stockProgramChartList.reversed()
    }

    fun searchStocks(
        keyword: String
    ): List<StockSearchItem> {
        val marketTypes = listOf("0", "10", "8", "60")
        return marketTypes
            .flatMap { mrktTp ->
                stockClient.stockInfoList(StockInfoListReq(mrkt_tp = mrktTp))?.list ?: emptyList()
            }
            .filter { it.name?.contains(keyword, ignoreCase = true) == true }
            .distinctBy { it.code }
            .map { StockSearchItem(stkCd = it.code!! + "_AL", stkNm = it.name!!, marketName = it.marketName!!) }
            .take(20)
    }

    fun streamStocks(
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
                        type = listOf("0B", "0H")
                    )
                )
            )
        )
    }

}