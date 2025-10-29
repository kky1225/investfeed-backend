package com.example.investfeed.kiwoom.stock.entity.res

data class KiwoomStockTradeValueRes(
    var stk_cd: String? = null, // 종목코드
    var now_rank: String? = null, // 현재순위
    var pred_rank: String? = null, // 전일순위
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var sel_bid: String? = null, // 매도호가
    var buy_bid: String? = null, // 매수호가
    var now_trde_qty: String? = null, // 현재거래량
    var pred_trde_qty: String? = null, // 전일거래량
    var trde_prica: String? = null, // 거래대금
)