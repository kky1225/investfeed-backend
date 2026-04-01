package com.example.investfeed.kiwoom.holding.dto.req

data class KiwoomHoldingReq(
    var qry_tp: String, // 조회구분 1:합산, 2:개별
    var dmst_stex_tp: String // 국내거래소구분 KRX:한국거래소, NXT:넥스트트레이드
)
