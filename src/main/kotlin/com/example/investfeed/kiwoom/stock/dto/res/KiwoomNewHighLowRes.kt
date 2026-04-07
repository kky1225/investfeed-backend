package com.example.investfeed.kiwoom.stock.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomNewHighLowRes(
    override var return_code: Int,
    override var return_msg: String,
    var ntl_pric: List<KiwoomNewHighLowItem>? = null,
) : KiwoomRes(return_code, return_msg)

data class KiwoomNewHighLowItem(
    var stk_cd: String? = null,
    var stk_nm: String? = null,
    var cur_prc: String? = null,
    var pred_pre_sig: String? = null,
    var pred_pre: String? = null,
    var flu_rt: String? = null,
    var trde_qty: String? = null,
    var pred_trde_qty_pre_rt: String? = null,
    var sel_bid: String? = null,
    var buy_bid: String? = null,
    var high_pric: String? = null,
    var low_pric: String? = null,
)
