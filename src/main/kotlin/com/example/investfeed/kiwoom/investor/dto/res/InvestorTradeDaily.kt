package com.example.investfeed.kiwoom.investor.dto.res

data class InvestorTradeDaily(
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var netslmt_qty: String? = null, // 순매도수량
    var netslmt_amt: String? = null, // 순매도금액
    var prsm_avg_pric: String? = null, // 추정평균가
    var cur_prc: String? = null, // 현재가
    var pre_sig: String? = null, // 대비기호
    var pred_pre: String? = null, // 전일대비
    var avg_pric_pre: String? = null, // 평균가대비
    var pre_rt: String? = null, // 대비율
    var dt_trde_qty: String? = null // 기간거래량
)