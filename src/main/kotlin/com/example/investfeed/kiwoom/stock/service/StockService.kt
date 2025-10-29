package com.example.investfeed.kiwoom.stock.service

import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.StockListReq
import com.example.investfeed.kiwoom.stock.dto.res.StockListItemRes
import com.example.investfeed.kiwoom.stock.dto.res.StockListRes
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockTradeValueReq
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockTradeVolumeListReq
import org.springframework.stereotype.Service

@Service
class StockService(
    private val socketClient: StockClient
) {
    fun stockList(
        req: StockListReq
    ): StockListRes? {
        when (req.type) {
            "0" -> {
                val kiwoomStockTradeValueListRes = socketClient.stockTradeValueList(
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
                val kiwoomStockTradeVolumeListRes = socketClient.stockTradeVolumeList(
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
                val kiwoomStockTradeValueListRes = socketClient.stockTradeValueList(
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
        }
    }
}