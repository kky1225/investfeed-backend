package com.example.investfeed.kiwoom.stock.service

import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.res.StockListItemRes
import com.example.investfeed.kiwoom.stock.dto.res.StockListRes
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockTradeHighReq
import org.springframework.stereotype.Service

@Service
class StockService(
    private val socketClient: StockClient
) {
    fun stockList(): StockListRes? {
        val kiwoomStockTradeHighListRes = socketClient.stockTradeHighList(
            req = KiwoomStockTradeHighReq(
                mrkt_tp = "000",
                mang_stk_incls = "1",
                stex_tp = "3"
            )
        )

        return StockListRes(
            return_code = kiwoomStockTradeHighListRes.return_code,
            return_msg = kiwoomStockTradeHighListRes.return_msg,
            stockList = kiwoomStockTradeHighListRes.trde_prica_upper?.map {
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