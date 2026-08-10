package com.example.investfeed.domain.us.sect.service

import com.example.investfeed.domain.us.sect.dto.req.UsSectStockListReq
import com.example.investfeed.domain.us.sect.dto.req.UsSectStockStreamReq
import com.example.investfeed.domain.us.sect.dto.res.UsSectListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectListRes
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListItem
import com.example.investfeed.domain.us.sect.dto.res.UsSectStockListRes
import com.example.investfeed.kiwoom.us.sect.client.UsSectClient
import com.example.investfeed.kiwoom.us.sect.dto.req.KiwoomUsSectPerformanceListReq
import com.example.investfeed.kiwoom.us.sect.dto.req.KiwoomUsSectStockListReq
import com.example.investfeed.kiwoom.us.stock.client.UsStockSocketClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStream
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamItem
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamReq
import org.springframework.stereotype.Service

@Service
class UsSectService(
    private val usSectClient: UsSectClient,
    private val usStockSocketClient: UsStockSocketClient,
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

        return UsSectStockListRes(
            sectStockList = kiwoomUsSectStockListRes.result_list?.map {
                UsSectStockListItem(
                    stkCd = it.stk_cd,
                    stexTp = it.stex_tp,
                    stkNm = it.stk_nm,
                    fluRt = it.flu_rt,
                    curPrc = it.cur_prc,
                    predPreSig = it.pred_pre_sig,
                    accTrdeQty = it.acc_trde_qty,
                )
            } ?: emptyList()
        )
    }

    fun streamUsStocks(
        req: UsSectStockStreamReq
    ) {
        usStockSocketClient.usStockListStream(
            req = KiwoomUsStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomUsStockStream(
                        item = req.items.map {
                            KiwoomUsStockStreamItem(
                                jmcode = it.stkCd,
                                stex_tp = it.stexTp
                            )
                        },
                        type = listOf("FE")
                    )
                )
            )
        )
    }
}
