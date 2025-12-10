package com.example.investfeed.kiwoom.stock.entity.res

data class KiwoomStockTradeInfo(
    val tm: String? = null, // 시간
    val cur_prc: String? = null, // 현재가
    val pred_pre: String? = null, // 전일대비
    val pre_rt: String? = null, // 대비율
    val pri_sel_bid_unit: String? = null, // 우선매도호가단위
    val pri_buy_bid_unit: String? = null, // 우선매수호가단위
    val cntr_trde_qty: String? = null, // 체결거래량
    val sign: String? = null, // sign
    val acc_trde_qty: String? = null, // 누적거래량
    val acc_trde_prica: String? = null, // 누적거래대금
    val cntr_str: String? = null, // 체결강도
    val stex_tp: String? = null, // 거래소구분 KRX, NXT, 통합
)