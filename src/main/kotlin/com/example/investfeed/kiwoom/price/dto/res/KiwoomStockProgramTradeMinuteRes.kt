package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockProgramTradeMinuteRes(
    override var return_code: Int,
    override var return_msg: String,
    var stk_tm_prm_trde_trnsn: List<KiwoomStockProgramTradeMinute>? = null // 종목시간별프로그램매매추이
): KiwoomRes(return_code = return_code, return_msg = return_msg)
