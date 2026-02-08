package com.example.investfeed.kiwoom.rank.dto.res

data class KiwoomInvestorTrade(
    var for_netslmt_stk_cd: String? = null, // 외인순매도종목코드
    var for_netslmt_stk_nm: String? = null, // 외인순매도종목명
    var for_netslmt_amt: String? = null, // 외인순매도금액
    var for_netslmt_qty: String? = null, // 외인순매도수량
    var for_netprps_stk_cd: String? = null, // 외인순매수종목코드
    var for_netprps_stk_nm: String? = null, // 외인순매수종목명
    var for_netprps_amt: String? = null, // 외인순매수금액
    var for_netprps_qty: String? = null, // 외인순매수수량
    var orgn_netslmt_stk_cd: String? = null, // 기관순매도종목코드
    var orgn_netslmt_stk_nm: String? = null, // 기관순매도종목명
    var orgn_netslmt_amt: String? = null, // 기관순매도금액
    var orgn_netslmt_qty: String? = null, // 기관순매도수량
    var orgn_netprps_stk_cd: String? = null, // 기관순매수종목코드
    var orgn_netprps_stk_nm: String? = null, // 기관순매수종목명
    var orgn_netprps_amt: String? = null, // 기관순매수금액
    var orgn_netprps_qty: String? = null, // 기관순매수수량
)