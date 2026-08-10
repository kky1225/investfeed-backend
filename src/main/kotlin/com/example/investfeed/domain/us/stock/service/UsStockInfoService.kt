package com.example.investfeed.domain.us.stock.service

import com.example.investfeed.domain.us.stock.dto.req.UsStockChartType
import com.example.investfeed.domain.us.stock.dto.req.UsStockDetailReq
import com.example.investfeed.domain.us.stock.dto.res.UsStockChart
import com.example.investfeed.domain.us.stock.dto.res.UsStockChartRes
import com.example.investfeed.domain.us.stock.dto.res.UsStockDailyPrice
import com.example.investfeed.domain.us.stock.dto.res.UsStockDetailRes
import com.example.investfeed.domain.us.stock.dto.res.UsStockInfo
import com.example.investfeed.domain.us.stock.dto.res.UsStockSearchItem
import com.example.investfeed.domain.us.stock.entity.UsStockMaster
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.kiwoom.us.chart.client.UsStockChartClient
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import com.example.investfeed.kiwoom.us.chart.dto.res.KiwoomUsStockChartRes
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.client.UsStockSocketClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoListReq
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStream
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamItem
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamReq
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsStockInfoService(
    private val usStockClient: UsStockClient,
    private val usStockChartClient: UsStockChartClient,
    private val usStockSocketClient: UsStockSocketClient,
    private val usStockMasterRepository: UsStockMasterRepository,
) {
    private val log = KotlinLogging.logger {}

    fun searchUsStocks(
        keyword: String
    ): List<UsStockSearchItem> {
        return usStockMasterRepository.search(keyword, PageRequest.of(0, 20))
            .map {
                UsStockSearchItem(
                    stkCd = it.stkCd,
                    stkNm = it.stkNm ?: it.stkEnm ?: it.stkCd,
                    stexTp = it.stexTp,
                    marketName = it.mkgb ?: it.stexTp,
                )
            }
    }

    fun getStkNm(
        stexTp: String,
        stkCd: String
    ): String? =
        usStockMasterRepository.findByStexTpAndStkCd(stexTp, stkCd)
            ?.let { it.stkNm ?: it.stkEnm }

    private fun fetchChartRes(
        stkCd: String,
        req: UsStockDetailReq
    ): KiwoomUsStockChartRes {
        val chartReq = KiwoomUsStockChartReq(
            stex_tp = req.stexTp,
            stk_cd = stkCd,
            tic_scope = req.chartType.ticScope
        )

        return when (req.chartType) {
            UsStockChartType.DAY -> usStockChartClient.usStockDayChart(chartReq)
            UsStockChartType.WEEK -> usStockChartClient.usStockWeekChart(chartReq)
            UsStockChartType.MONTH -> usStockChartClient.usStockMonthChart(chartReq)
            UsStockChartType.YEAR -> usStockChartClient.usStockYearChart(chartReq)
            else -> usStockChartClient.usStockMinuteChart(chartReq)
        }
    }

    private fun mapChartList(chartRes: KiwoomUsStockChartRes): List<UsStockChart> =
        (chartRes.result_list ?: emptyList())
            .map {
                UsStockChart(
                    dt = it.cntr_tm ?: it.dt,
                    curPrc = it.cur_prc,
                    openPric = it.open_pric,
                    highPric = it.high_pric,
                    lowPric = it.low_pric,
                    trdeQty = it.trde_qty ?: it.acc_trde_qty,
                    trdePrica = it.acc_trde_prica,
                )
            }
            .reversed()

    fun getUsStockChart(
        stkCd: String,
        req: UsStockDetailReq
    ): UsStockChartRes {
        val info = usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = req.stexTp, stk_cd = stkCd))

        return UsStockChartRes(
            usStockInfo = mapUsStockInfo(info),
            chartList = mapChartList(fetchChartRes(stkCd, req)),
        )
    }

    fun getUsStockDetail(
        stkCd: String,
        req: UsStockDetailReq
    ): UsStockDetailRes {
        val info = usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = req.stexTp, stk_cd = stkCd))

        val chartRes = fetchChartRes(stkCd, req)
        val chartList = mapChartList(chartRes)

        val dailyPriceList = try {
            val dayChartRes = if (req.chartType == UsStockChartType.DAY) chartRes
                else usStockChartClient.usStockDayChart(KiwoomUsStockChartReq(stex_tp = req.stexTp, stk_cd = stkCd))

            (dayChartRes.result_list ?: emptyList())
                .map {
                    val fluRt = it.flu_rt ?: ""
                    UsStockDailyPrice(
                        dt = it.dt,
                        curPrc = it.cur_prc,
                        predPreSig = when {
                            fluRt.startsWith("-") -> "5"
                            (fluRt.replace("+", "").toDoubleOrNull() ?: 0.0) > 0.0 -> "2"
                            else -> "3"
                        },
                        predPre = it.pred_pre,
                        fluRt = it.flu_rt,
                        openPric = it.open_pric,
                        highPric = it.high_pric,
                        lowPric = it.low_pric,
                        accTrdeQty = it.acc_trde_qty,
                        trdePrica = it.acc_trde_prica,
                    )
                }
        } catch (e: Exception) {
            log.error { "미국 주식 일별 시세 조회 실패 : stkCd=$stkCd, ${e.message}" }
            emptyList()
        }

        return UsStockDetailRes(
            usStockInfo = mapUsStockInfo(info),
            chartList = chartList,
            dailyPriceList = dailyPriceList,
        )
    }

    private fun mapUsStockInfo(info: KiwoomUsStockInfoRes): UsStockInfo =
        UsStockInfo(
            stexTp = info.stex_tp,
            stkCd = info.stk_cd,
            stkNm = info.stk_nm,
            stkEnm = info.stk_enm,
            curPrc = info.cur_prc,
            predPreSig = info.pred_pre_sig,
            predPre = info.pred_pre,
            fluRt = info.flu_rt,
            accTrdeQty = info.acc_trde_qty,
            baseExrt = info.base_exrt,
            wk52HgstPric = info.wk52_hgst_pric,
            wk52HgstPricDt = info.wk52_hgst_pric_dt,
            wk52HgstPricPreRt = info.wk52_hgst_pric_pre_rt,
            wk52LwstPric = info.wk52_lwst_pric,
            wk52LwstPricDt = info.wk52_lwst_pric_dt,
            wk52LwstPricPreRt = info.wk52_lwst_pric_pre_rt,
            preOpenPric = info.pre_open_pric,
            preHighPric = info.pre_high_pric,
            preLowPric = info.pre_low_pric,
            baseClosePric = info.base_close_pric,
            openPric = info.open_pric,
            highPric = info.high_pric,
            lowPric = info.low_pric,
            stkCnt = info.stk_cnt,
            mac = info.mac,
            lgIndsCd = info.lg_inds_cd,
            smIndsCd = info.sm_inds_cd,
            currUnit = info.curr_unit,
            trdSuspTp = info.trd_susp_tp,
        )

    fun streamUsStock(stkCd: String, stexTp: String) {
        usStockSocketClient.usStockListStream(
            req = KiwoomUsStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomUsStockStream(
                        item = listOf(
                            KiwoomUsStockStreamItem(
                                jmcode = stkCd,
                                stex_tp = stexTp
                            )
                        ),
                        type = listOf("FE")
                    )
                )
            )
        )
    }

    @Transactional
    fun syncAll(): Int {
        val masters = (usStockClient.usStockInfoList(KiwoomUsStockInfoListReq(stex_tp = "%")).list ?: emptyList())
            .distinctBy { "${it.stex_tp}|${it.stk_cd}" }
            .mapNotNull {
                val stexTp = it.stex_tp ?: return@mapNotNull null
                val stkCd = it.stk_cd ?: return@mapNotNull null
                UsStockMaster(
                    stexTp = stexTp,
                    stkCd = stkCd,
                    stkNm = it.stk_nm,
                    stkEnm = it.stk_enm,
                    mkgb = it.mkgb,
                    upgb = it.upgb,
                    isEtf = it.isEtf,
                )
            }

        usStockMasterRepository.deleteAllInBatch()
        usStockMasterRepository.saveAll(masters)

        return masters.size
    }
}
