package com.example.investfeed.kiwoom.gold.dto.rest.res

data class GoldInvestor(
    var all_dfrt_trst_sell_qty: String? = null, // 투자자별 매도 수량(천)
    var sell_qty_irds: String? = null, // 투자자별 매도 수량 증감(천)
    var all_dfrt_trst_sell_amt: String? = null, // 투자자별 매도 금액(억)
    var sell_amt_irds: String? = null, // 투자자별 매도 금액 증감(억)
    var all_dfrt_trst_buy_qty: String? = null, // 투자자별 매수 수량(천)
    var buy_qty_irds: String? = null, // 투자자별 매수 수량 증감(천)
    var all_dfrt_trst_buy_amt: String? = null, // 투자자별 매수 금액(억)
    var buy_amt_irds: String? = null, // 투자자별 매수 금액 증감(억)
    var all_dfrt_trst_netprps_qty: String? = null, // 투자자별 순매수 수량(천)
    var netprps_qty_irds: String? = null, // 투자자별 순매수 수량 증감(천)
    var all_dfrt_trst_netprps_amt: String? = null, //	투자자별 순매수 금액(억)
    var netprps_amt_irds: String? = null, // 투자자별 순매수 금액 증감(억)
    var sell_uv: String? = null, // 투자자별 매도 단가
    var buy_uv: String? = null, // 투자자별 매수 단가
    var stk_nm: String? = null, // 투자자 구분명
    var acc_netprps_amt: String? = null, // 누적 순매수 금액(억)
    var acc_netprps_qty: String? = null, // 누적 순매수 수량(천)
    var stk_cd: String? = null // 투자자 코드
)