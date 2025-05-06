package com.example.investfeed.kiwoom.sect.dto.res

data class SectPrice(
    var stk_cd: String,
    var stk_nm: String,
    var cur_prc: String,
    var pred_pre_sig: String,
    var pred_pre: String,
    var flu_rt: String,
    var now_trde_qty: String,
    var sel_bid: String,
    var buy_bid: String,
    var open_pric: String,
    var high_pric: String,
    var low_pric: String
)