package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomInvestorTradeCloseMarketReq(
    var mrkt_tp: String? = null, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액, 2:수량
    var trde_tp: String? = null, // 매매구분 0:순매수, 1:매수, 2:매도
    var stex_tp: String? = null, // 거래소구분 1:KRX, 2:NXT 3.통합
)