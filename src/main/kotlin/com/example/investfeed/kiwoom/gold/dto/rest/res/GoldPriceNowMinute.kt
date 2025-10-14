package com.example.investfeed.kiwoom.gold.dto.rest.res

data class GoldPriceNowMinute(
    var cntr_pric: String? = null, // 체결가
    var pred_pre: String? = null, // 전일 대비(원)
    var flu_rt: String? = null, // 등락율
    var trde_qty: String? = null, // 누적 거래량
    var acc_trde_prica: String? = null, // 누적 거래대금
    var cntr_trde_qty: String? = null, // 거래량(체결량)
    var tm: String? = null, // 체결시간
    var pre_sig: String? = null, // 전일대비기호
    var pri_sel_bid_unit: String? = null, // 매도호가
    var pri_buy_bid_unit: String? = null, // 매수호가
    var trde_pre: String? = null, // 전일 거래량 대비 비율
    var trde_tern_rt: String? = null, // 전일 거래량 대비 순간 거래량 비율
    var cntr_str: String? = null, // 체결강도
    var lpmmcm_nm_1: String? = null, // K.O 접근도
    var stex_tp: String? = null, // 거래소구분
)