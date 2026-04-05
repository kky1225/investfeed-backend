package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomStockProgramTradeMinute(
    var tm: String? = null, // 시간
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 거래량
    var prm_sell_amt: String? = null, // 프로그램매도금액
    var prm_buy_amt: String? = null, // 프로그램매수금액
    var prm_netprps_amt: String? = null, // 프로그램순매수금액
    var prm_netprps_amt_irds: String? = null, // 프로그램순매수금액증감
    var prm_sell_qty: String? = null, // 프로그램매도수량
    var prm_buy_qty: String? = null, // 프로그램매수수량
    var prm_netprps_qty: String? = null, // 프로그램순매수수량
    var prm_netprps_qty_irds: String? = null, // 프로그램순매수수량증감
)
