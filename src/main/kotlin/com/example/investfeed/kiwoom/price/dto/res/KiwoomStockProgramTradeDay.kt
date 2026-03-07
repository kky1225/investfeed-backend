package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomStockProgramTradeDay(
    var dt: String? = null, // 일자
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
    var base_pric_tm: String? = null, // 기준가시간
    var dbrt_trde_rpy_sum: String? = null, // 대차거래상환주수합
    var remn_rcvord_sum: String? = null, // 잔고수주합
    var stex_tp: String? = null, // 거래소구분 KRX , NXT , 통합
)