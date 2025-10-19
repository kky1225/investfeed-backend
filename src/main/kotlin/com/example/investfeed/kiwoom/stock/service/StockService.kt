package com.example.investfeed.kiwoom.stock.service

import com.example.investfeed.kiwoom.stock.client.StockClient
import com.example.investfeed.kiwoom.stock.dto.req.StockTradeHighListReq
import com.example.investfeed.kiwoom.stock.dto.res.StockTradeHighRes
import org.springframework.stereotype.Service

@Service
class StockService(
    private val socketClient: StockClient
) {
    fun stockList(): StockTradeHighRes? {
        return socketClient.stockTradeHighList(
            req = StockTradeHighListReq(
                mrkt_tp = "000",
                mang_stk_incls = "1",
                stex_tp = "3"
            )
        )
    }
}