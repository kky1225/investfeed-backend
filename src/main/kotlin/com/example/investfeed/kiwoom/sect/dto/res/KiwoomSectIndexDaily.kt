package com.example.investfeed.kiwoom.sect.dto.res

data class KiwoomSectIndexDaily(
    var dt_n: String? = null, // 일자 n
    var cur_prc_n: String? = null, // 현재가 n
    var pred_pre_sig_n: String? = null, // 전일대비기호 n
    var pred_pre_n: String? = null, // 전일대비 n
    var flu_rt_n: String? = null, // 등락률 n
    var acc_trde_qty_n: String? = null // 누적거래량 n
)