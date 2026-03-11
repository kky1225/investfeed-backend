package com.example.investfeed.kiwoom.chart.dto.stock.req

data class KiwoomStockChartInvestorReq(
    var mrkt_tp: String? = null, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액, 2:수량
    var trde_tp: String? = null, // 매매구분 0:순매수, 1:매수, 2:매도
    var stk_cd: String? = null, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
)