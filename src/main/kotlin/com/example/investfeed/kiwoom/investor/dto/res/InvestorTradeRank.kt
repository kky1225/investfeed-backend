package com.example.investfeed.kiwoom.investor.dto.res

data class InvestorTradeRank(
    var rank: String? = null, // 순위
    var stk_cd: String? = null, // 종목코드
    var stk_nm: String? = null, // 종목명
    var prid_stkpc_flu_rt: String? = null, // 기간중주가등락률
    var orgn_nettrde_amt: String? = null, // 기관순매매금액
    var orgn_nettrde_qty: String? = null, // 기관순매매량
    var orgn_cont_netprps_dys: String? = null, // 기관계연속순매수일수
    var orgn_cont_netprps_qty: String? = null, // 기관계연속순매수량
    var orgn_cont_netprps_amt: String? = null, // 기관계연속순매수금액
    var frgnr_nettrde_qty: String? = null, // 외국인순매매량
    var frgnr_nettrde_amt: String? = null, // 외국인순매매액
    var frgnr_cont_netprps_dys: String? = null, // 외국인연속순매수일수
    var frgnr_cont_netprps_qty: String? = null, // 외국인연속순매수량
    var frgnr_cont_netprps_amt: String? = null, // 외국인연속순매수금액
    var nettrde_qty: String? = null, // 순매매량
    var nettrde_amt: String? = null, // 순매매액
    var tot_cont_netprps_dys: String? = null, // 합계연속순매수일수
    var tot_cont_nettrde_qty: String? = null, // 합계연속순매매수량
    var tot_cont_netprps_amt: String? = null // 합계연속순매수금액
)