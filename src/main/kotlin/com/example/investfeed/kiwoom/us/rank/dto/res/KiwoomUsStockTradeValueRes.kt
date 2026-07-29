package com.example.investfeed.kiwoom.us.rank.dto.res

data class KiwoomUsStockTradeValueRes(
    var rank: String? = null, // 순위
    var stex_tp: String? = null, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var stk_enm: String? = null, // 종목영문명
    var cur_prc: String? = null, // 현재가 (USD)
    var pred_pre_sig: String? = null, // 전일대비기호 1:상한가, 2:상승, 3:보합, 4:하한가, 5:하락
    var pred_pre: String? = null, // 전일대비 (USD)
    var flu_rt: String? = null, // 등락률 (%)
    var acc_trde_qty: String? = null, // 거래량 (1주)
    var pred_trde_qty: String? = null, // 전일거래량 (1주)
    var trde_prica: String? = null, // 거래대금 (천USD)
)
