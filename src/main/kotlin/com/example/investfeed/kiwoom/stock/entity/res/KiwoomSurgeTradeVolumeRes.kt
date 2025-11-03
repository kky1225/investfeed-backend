package com.example.investfeed.kiwoom.stock.entity.res

data class KiwoomSurgeTradeVolumeRes(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var prev_trde_qty: String? = null, // 이전거래량
    var now_trde_qty: String? = null, // 현재거래량
    var sdnin_qty: String? = null, // 급증량
    var sdnin_rt: String? = null // 급증률
)