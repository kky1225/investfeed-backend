package com.example.investfeed.kiwoom.sect.dto.res

data class SectPriceNowTime (
    var tm_n: String? = null, // 시간 n
    var cur_prc_n: String? = null, // 현재가 n
    var pred_pre_sig_n: String? = null, // 전일대비기호 n
    var pred_pre_n: String? = null, // 전일대비 n
    var flt_rt_n: String? = null, // 등락률 n
    var trde_qty_n: String? = null, // 거래량 n
    var acc_trde_qty_n: String? = null // 누적거래량 n
)