package com.example.investfeed.domain.us.sect.service

import com.example.investfeed.domain.us.sect.dto.req.UsSectStockListReq
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockStreamReq
import com.example.investfeed.domain.us.sect.dto.res.UsSectListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectListRes
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListRes
import com.example.investfeed.domain.us.stock.service.UsEtfService
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
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
    private val usSectClient: UsSectClient,
    private val usEtfService: UsEtfService,
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

        return UsSectStockListRes(
            sectStockList = rows.map {
                UsSectStockListItem(
                    stkCd = it.stk_cd,
                    stexTp = it.stex_tp,
                    stkNm = usEtfService.displayName(it.stk_cd, it.stk_nm, etfTickers),
                    fluRt = it.flu_rt,
                    curPrc = it.cur_prc,
                    predPreSig = it.pred_pre_sig,
                    accTrdeQty = it.acc_trde_qty,
                )
            }
        )
    }

    fun streamUsStocks(
        req: UsSectStockStreamReq
    ) {
        kiwoomStreamClient.register(
            StreamEntry(
                market = StreamMarket.US,
                items = req.items.map { KiwoomUsStreamItem(jmcode = it.stkCd, stex_tp = it.stexTp) },
                types = listOf("FE")
            )
        )
    }
}
