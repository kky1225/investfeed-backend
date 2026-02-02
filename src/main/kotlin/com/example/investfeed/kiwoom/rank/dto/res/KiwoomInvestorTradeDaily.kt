package com.example.investfeed.kiwoom.rank.dto.res

data class KiwoomInvestorTradeDaily(
    var stk_cd: String? = null, //종목코드
    var stk_nm: String? = null, //종목명
    var sel_qty: String? = null, //매도량
    var buy_qty: String? = null, //매수량
    var netslmt: String? = null, //순매도
)