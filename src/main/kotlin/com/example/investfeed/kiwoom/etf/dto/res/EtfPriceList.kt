package com.example.investfeed.kiwoom.etf.dto.res

data class EtfPriceList(
    var stk_cd: String? = null, // 종목코드
    var stk_cls: String? = null, // 종목분류
    var stk_nm: String? = null, // 종목명
    var close_pric: String? = null, // 종가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var pre_rt: String? = null, // 대비율
    var trde_qty: String? = null, // 거래량
    var nav: String? = null, // NAV
    var trace_eor_rt: String? = null, // 추적오차율
    var txbs: String? = null, // 과표기준
    var dvid_bf_base: String? = null, // 배당전기준
    var pred_dvida: String? = null, // 전일배당금
    var trace_idex_nm: String? = null, // 추적지수명
    var drng: String? = null, // 배수
    var trace_idex_cd: String? = null, // 추적지수코드
    var trace_idex: String? = null, // 추적지수
    var trace_flu_rt: String? = null, // 추적등락율
)