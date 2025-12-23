package com.example.investfeed.kiwoom.rank.dto.req

data class KiwoomStockTradeValueListReq(
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var mang_stk_incls: String, // 관리종목포함 0:관리종목 미포함, 1:관리종목 포함
    var stex_tp: String, // 거래소구분 1:KRX, 2:NXT 3.통합
)