package com.example.investfeed.kiwoom.sect.dto.res

data class SectPrice(
    var stk_cd: String, // 종목코드
    var stk_nm: String, // 종목명
    var cur_prc: String, // 현재가
    var pred_pre_sig: String, // 전일대비기호
    var pred_pre: String, // 전일대비
    var flu_rt: String, // 등락률
    var now_trde_qty: String, // 현재거래량
    var sel_bid: String, // 매도호가
    var buy_bid: String, // 매수호가
    var open_pric: String, // 시가
    var high_pric: String, // 고가
    var low_pric: String // 저가
)