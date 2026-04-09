package com.example.investfeed.kiwoom.realizedpnl.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomRealizedPnlRes(
    override var return_code: Int,
    override var return_msg: String,
    var tot_buy_amt: String? = null,  // 총매수금액
    var tot_sell_amt: String? = null, // 총매도금액
    var rlzt_pl: String? = null,     // 실현손익
    var trde_cmsn: String? = null,   // 매매수수료
    var trde_tax: String? = null,    // 매매세금
    var dt_rlzt_pl: List<KiwoomDailyRealizedPnl>? = null // 일자별실현손익
) : KiwoomRes(return_code, return_msg)

data class KiwoomDailyRealizedPnl(
    var dt: String? = null,            // 일자 YYYYMMDD
    var buy_amt: String? = null,       // 매수금액
    var sell_amt: String? = null,      // 매도금액
    var tdy_sel_pl: String? = null,    // 당일매도손익
    var tdy_trde_cmsn: String? = null, // 당일매매수수료
    var tdy_trde_tax: String? = null   // 당일매매세금
)
