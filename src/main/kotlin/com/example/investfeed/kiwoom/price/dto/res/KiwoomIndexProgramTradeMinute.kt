package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomIndexProgramTradeMinute(
    var cntr_tm: String? = null, // 체결시간
    var dfrt_trde_sel: String? = null, // 차익거래매도
    var dfrt_trde_buy: String? = null, // 차익거래매수
    var dfrt_trde_netprps: String? = null, // 차익거래순매수
    var ndiffpro_trde_sel: String? = null, // 비차익거래매도
    var ndiffpro_trde_buy: String? = null, // 비차익거래매수
    var ndiffpro_trde_netprps: String? = null, // 비차익거래순매수
    var dfrt_trde_sell_qty: String? = null, // 차익거래매도수량
    var dfrt_trde_buy_qty: String? = null, // 차익거래매수수량
    var dfrt_trde_netprps_qty: String? = null, // 차익거래순매수수량
    var ndiffpro_trde_sell_qty: String? = null, // 비차익거래매도수량
    var ndiffpro_trde_buy_qty: String? = null, // 비차익거래매수수량
    var ndiffpro_trde_netprps_qty: String? = null, // 비차익거래순매수수량
    var all_sel: String? = null, // 전체매도
    var all_buy: String? = null, // 전체매수
    var all_netprps: String? = null, // 전체순매수
    var kospi200: String? = null, // KOSPI200
    var basis: String? = null, // BASIS
)