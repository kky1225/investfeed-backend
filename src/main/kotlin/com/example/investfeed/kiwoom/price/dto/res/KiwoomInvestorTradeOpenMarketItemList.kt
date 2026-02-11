package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomInvestorTradeOpenMarketItemList(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var acc_trde_qty: String? = null, // 누적거래량
    var netprps_amt: String? = null, // 순매수금액
    var prev_netprps_amt: String? = null, // 이전순매수금액
    var buy_amt: String? = null, // 매수금액
    var netprps_amt_irds: String? = null, // 순매수금액증감
    var buy_amt_irds: String? = null, // 매수금액증감
    var sell_amt: String? = null, // 매도금액
    var sell_amt_irds: String? = null, // 매도금액증감
    var netprps_qty: String? = null, // 순매수수량
    var prev_pot_netprps_qty: String? = null, // 이전시점순매수수량
    var netprps_irds: String? = null, // 순매수증감
    var buy_qty: String? = null, // 매수수량
    var buy_qty_irds: String? = null, // 매수수량증감
    var sell_qty: String? = null, // 매도수량
    var sell_qty_irds: String? = null, // 매도수량증감
)