package com.example.investfeed.kiwoom.us.sect.dto.res

data class KiwoomUsSectStockRes(
    var stex_tp: String? = null, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 한글종목명
    var stk_enm: String? = null, // 영문종목명
    var cur_prc: String? = null, // 현재가 (USD)
    var pred_pre_sig: String? = null, // 전일대비기호 1:상한가, 2:상승, 3:보합, 4:하한가, 5:하락
    var pred_pre: String? = null, // 전일대비 (USD)
    var flu_rt: String? = null, // 등락률 (%)
    var acc_trde_qty: String? = null, // 거래량 (1주)
    var sel_bid: String? = null, // 매도호가 (USD)
    var buy_bid: String? = null, // 매수호가 (USD)
    var open_pric: String? = null, // 시가 (USD)
    var high_pric: String? = null, // 고가 (USD)
    var low_pric: String? = null, // 저가 (USD)
    var cntr_tm: String? = null, // 시간 HH:mm (미국 동부시간)
)
