package com.example.investfeed.kiwoom.etf.dto.res

data class EtfTradeDailyList(
    var dt: String? = null, // 일자
    var cur_prc_n: String? = null, // 현재가 n
    var pre_sig_n: String? = null, // 대비기호 n
    var pred_pre_n: String? = null, // 전일대비 n
    var acc_trde_qty: String? = null, // 누적거래량
    var for_netprps_qty: String? = null, // 외인순매수수량
    var orgn_netprps_qty: String? = null, // 기관순매수수량
)