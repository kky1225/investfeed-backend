package com.example.investfeed.kiwoom.theme.dto.res

data class ThemeGroupStockList(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var cur_prc: String? = null, // 현재가
    var flu_sig: String? = null, // 등락기호
    var pred_pre: String? = null, // 전일대비
    var flu_rt: String? = null, // 등락율
    var acc_trde_qty: String? = null, // 누적거래량
    var sel_bid: String? = null, // 매도호가
    var sel_req: String? = null, // 매도잔량
    var buy_bid: String? = null, // 매수호가
    var buy_req: String? = null, // 매수잔량
    var dt_prft_rt_n: String? = null // 기간수익률 n
)