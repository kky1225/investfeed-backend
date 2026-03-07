package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomStockProgramTradeDayRes(
    override var return_code: Int,
    override var return_msg: String,
    var stk_daly_prm_trde_trnsn: List<KiwoomStockProgramTradeDay>? = null // 종목일별프로그램매매추이
): KiwoomRes(return_code = return_code, return_msg = return_msg)