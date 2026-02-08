package com.example.investfeed.kiwoom.rank.dto.req

data class KiwoomInvestorTradeReq(
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var amt_qty_tp: String, // 금액수량구분 1:금액(천만), 2:수량(천)
    var qry_dt_tp: String, // 조회일자구분 0:조회일자 미포함, 1:조회일자 포함
    var date: String? = null, // 날짜 YYYYMMDD(연도4자리, 월 2자리, 일 2자리 형식)
    var stex_tp: String, // 거래소구분 1:KRX, 2:NXT, 3:통합
)