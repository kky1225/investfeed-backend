package com.example.investfeed.domain.us.sect.service

import com.example.investfeed.domain.us.sect.dto.req.UsSectStockListReq
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockStreamReq
import com.example.investfeed.domain.us.sect.dto.res.UsSectListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectListRes
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListRes
import com.example.investfeed.domain.us.stock.service.UsEtfService
import com.example.investfeed.domain.us.stock.service.UsSessionBaseService
import kotlinx.coroutines.runBlocking
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import com.example.investfeed.toss.socket.TossStreamClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomUsStreamItem
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import com.example.investfeed.kiwoom.us.sect.client.UsSectClient
import com.example.investfeed.kiwoom.us.sect.dto.req.KiwoomUsSectPerformanceListReq
import com.example.investfeed.kiwoom.us.sect.dto.req.KiwoomUsSectStockListReq
import org.springframework.stereotype.Service

@Service
class UsSectService(
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val tossStreamClient: TossStreamClient,
    private val usSectClient: UsSectClient,
    private val usEtfService: UsEtfService,
    private val usSessionBaseService: UsSessionBaseService,
) {
    fun listUsSects(): UsSectListRes {
        val kiwoomUsSectPerformanceListRes = usSectClient.usSectPerformanceList(
            req = KiwoomUsSectPerformanceListReq(
                stex_tp = "0",
                inds_cd = "0"
            )
        )

        return UsSectListRes(
            sectList = kiwoomUsSectPerformanceListRes.result_list?.map {
                UsSectListItem(
                    indsCd = it.inds_cd,
                    indsNm = it.inds_nm,
                    perf1d = it.perf_1d,
                    perf5d = it.perf_5d,
                    perf1m = it.perf_1m,
                    perf3m = it.perf_3m,
                    perf6m = it.perf_6m,
                    perfYtd = it.perf_ytd,
                    perf1y = it.perf_1y,
                )
            } ?: emptyList()
        )
    }

    fun listStocksBySect(
        indsCd: String,
        req: UsSectStockListReq
    ): UsSectStockListRes {
        val kiwoomUsSectStockListRes = usSectClient.usSectStockList(
            req = KiwoomUsSectStockListReq(
                stex_tp = "0",
                sort_tp = req.sortTp,
                inds_cd = indsCd
            )
        )

        val rows = kiwoomUsSectStockListRes.result_list ?: emptyList()
        val etfTickers = usEtfService.etfTickers(rows.mapNotNull { it.stk_cd })

        // 순서는 키움 등락률 순 그대로. 토스 연동 회원은 실시간이 토스 기준이라 첫 등락률도 같은 기준으로 맞춘다
        val changes = runBlocking {
            usSessionBaseService.streamPriceChanges(
                rows.filter { it.stk_cd != null }.associate { it.stk_cd!! to it.cur_prc?.trimStart('+', '-')?.toDoubleOrNull() }
            )
        }

        return UsSectStockListRes(
            sectStockList = rows.map {
                val change = changes[it.stk_cd]
                UsSectStockListItem(
                    stkCd = it.stk_cd,
                    stexTp = it.stex_tp,
                    stkNm = usEtfService.displayName(it.stk_cd, it.stk_nm, etfTickers),
                    fluRt = change?.fluRt ?: it.flu_rt,
                    curPrc = it.cur_prc,
                    predPreSig = change?.predPreSig ?: it.pred_pre_sig,
                    accTrdeQty = it.acc_trde_qty,
                )
            }
        )
    }

    fun streamUsStocks(
        req: UsSectStockStreamReq
    ) {
        val usItems = if (tossStreamClient.register(req.items.map { it.stkCd })) emptyList() else req.items
        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.US,
                items = usItems.map { KiwoomUsStreamItem(jmcode = it.stkCd, stex_tp = it.stexTp) },
                types = listOf("FE")
            )
        )
    }
}
