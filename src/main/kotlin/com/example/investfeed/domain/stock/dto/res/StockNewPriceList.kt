package com.example.investfeed.domain.stock.dto.res

data class StockNewPriceList(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pred_pre_sig: String? = null, // 전일대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락률
    var trde_qty: String? = null, // 거래량
    var pred_trde_qty_pre_rt: String? = null, // 전일거래량대비율
    var sel_bid: String? = null, // 매도호가
    var buy_bid: String? = null, // 매수호가
    var high_pric: String? = null, // 고가
    var low_pric: String? = null // 저가
)