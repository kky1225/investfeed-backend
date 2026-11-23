package com.example.investfeed.domain.us.stock.service

import com.example.investfeed.common.util.failure
import com.example.investfeed.domain.us.stock.dto.req.UsStockChartType
import com.example.investfeed.domain.us.stock.dto.req.UsStockDetailReq
import com.example.investfeed.domain.us.stock.dto.res.UsSessionBase
import com.example.investfeed.domain.us.stock.dto.res.UsStockChart
import com.example.investfeed.domain.us.stock.dto.res.UsStockChartRes
import com.example.investfeed.domain.us.stock.dto.res.UsStockDailyPrice
import com.example.investfeed.domain.us.stock.dto.res.UsStockDetailRes
import com.example.investfeed.domain.us.stock.dto.res.UsStockInfo
import com.example.investfeed.domain.us.stock.dto.res.UsStockSearchItem
import com.example.investfeed.domain.us.stock.entity.UsStockMaster
import com.example.investfeed.domain.us.stock.repository.UsStockMasterRepository
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.toss.socket.TossStreamClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomUsStreamItem
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.example.investfeed.domain.us.stock.dto.req.UsStockCandleReq
import com.example.investfeed.domain.us.stock.dto.res.UsStockCandleRes
import com.example.investfeed.kiwoom.us.chart.client.UsChartPeriod
import com.example.investfeed.kiwoom.us.chart.client.UsStockChartClient
import com.example.investfeed.kiwoom.us.chart.dto.req.KiwoomUsStockChartReq
import com.example.investfeed.kiwoom.us.chart.dto.res.KiwoomUsStockChartRes
import com.example.investfeed.kiwoom.us.stock.client.UsStockClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoListReq
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockInfoReq
import com.example.investfeed.kiwoom.us.stock.dto.res.KiwoomUsStockInfoRes
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import com.example.investfeed.domain.watchlist.service.WatchlistService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsStockInfoService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val tossStreamClient: TossStreamClient,
    private val usStockClient: UsStockClient,
    private val usStockChartClient: UsStockChartClient,
    private val usStockMasterRepository: UsStockMasterRepository,
    private val usSessionBaseService: UsSessionBaseService,
    private val watchlistService: WatchlistService,
) {
    private val log = KotlinLogging.logger {}

    fun searchUsStocks(
        keyword: String
    ): List<UsStockSearchItem> {
        return usStockMasterRepository.search(keyword, PageRequest.of(0, 20))
            .map {
                // ETF 는 표시명 자리에 티커를 담는다(일반주는 종목명 그대로)
                val stkNm = if (it.isEtf == "Y") it.stkCd else (it.stkNm ?: it.stkEnm ?: it.stkCd)
                UsStockSearchItem(
                    stkCd = it.stkCd,
                    stkNm = stkNm,
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

    /** 차트의 과거 구간 한 페이지. cursor 없이 부르면 가장 최근 구간, 받은 nextCursor 를 넣으면 그 이전 구간 */
    fun getUsStockCandles(stkCd: String, req: UsStockCandleReq): UsStockCandleRes {
        val period = when (req.chartType) {
            UsStockChartType.DAY -> UsChartPeriod.DAY
            UsStockChartType.WEEK -> UsChartPeriod.WEEK
            UsStockChartType.MONTH -> UsChartPeriod.MONTH
            UsStockChartType.YEAR -> UsChartPeriod.YEAR
            else -> UsChartPeriod.MINUTE
        }
        val (chartRes, nextKey) = usStockChartClient.usStockChartPage(
            period = period,
            req = KiwoomUsStockChartReq(stex_tp = req.stexTp, stk_cd = stkCd, tic_scope = req.chartType.ticScope),
            nextKey = req.cursor,
        )
        return UsStockCandleRes(candles = mapChartList(chartRes), nextCursor = nextKey)
    }

    fun getUsStockChart(
        stkCd: String,
        req: UsStockDetailReq
    ): UsStockChartRes {
        val info = usStockClient.usStockInfo(KiwoomUsStockInfoReq(stex_tp = req.stexTp, stk_cd = stkCd))

        return UsStockChartRes(
            usStockInfo = mapUsStockInfo(info, resolveIsEtf(req.stexTp, stkCd)),
            chartList = mapChartList(fetchChartRes(stkCd, req)),
        )
    }

    fun getUsStockDetail(
        stkCd: String,
        req: UsStockDetailReq,
        memberId: Long,
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
            log.failure(e) { "미국 주식 일별 시세 조회 실패 : stkCd=$stkCd" }
            emptyList()
        }

        val sessionBase = runBlocking { usSessionBaseService.currentBases(listOf(stkCd)) }
            ?.let { b -> b.closes[stkCd]?.let { UsSessionBase(sessionLabel = b.session.label, regularClose = it) } }

        return UsStockDetailRes(
            usStockInfo = mapUsStockInfo(info, resolveIsEtf(req.stexTp, stkCd)),
            chartList = chartList,
            dailyPriceList = dailyPriceList,
            sessionBase = sessionBase,
            // 하트는 부가 정보라 조회에 실패해도 상세 전체를 실패시키지 않는다.
            watchlistGroupIds = try {
                watchlistService.groupIdsContaining(memberId, stkCd, req.stexTp)
            } catch (e: Exception) {
                log.failure(e) { "관심 그룹 조회 실패 : stkCd=$stkCd, stexTp=${req.stexTp}" }
                emptyList()
            },
        )
    }

    /** 키움 상세 응답에는 ETF 여부가 없어 마스터에서 조회한다. */
    private fun resolveIsEtf(stexTp: String, stkCd: String): Boolean =
        usStockMasterRepository.findByStexTpAndStkCd(stexTp, stkCd)?.isEtf == "Y"

    private fun mapUsStockInfo(info: KiwoomUsStockInfoRes, isEtf: Boolean): UsStockInfo =
        UsStockInfo(
            stexTp = info.stex_tp,
            stkCd = info.stk_cd,
            stkNm = info.stk_nm,
            stkEnm = info.stk_enm,
            isEtf = isEtf,
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
        val byToss = tossStreamClient.register(listOf(stkCd))
        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.US,
                items = if (byToss) emptyList() else listOf(KiwoomUsStreamItem(jmcode = stkCd, stex_tp = stexTp)),
                types = listOf("FE")
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
