package com.example.investfeed.kiwoom.investor.dto.req

data class InvestorTradeRankReq(
    var dt: String, // 기간 1:최근일, 3:3일, 5:5일, 10:10일, 20:20일, 120:120일, 0:시작일자/종료일자로 조회
    var strt_dt: String? = null, // YYYYMMDD
    var end_dt: String? = null, // YYYYMMDD
    var mrkt_tp: String, // 001:코스피, 101:코스닥
    var netslmt_tp: String = "2", // 2:순매수(고정값)
    var stk_inds_tp: String, // 0:종목(주식),1:업종
    var amt_qty_tp: String, // 0:금액, 1:수량
    var stex_tp: String, // 1:KRX, 2:NXT, 3:통합
)