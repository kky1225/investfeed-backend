package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomIndexProgramTradeMinuteRes(
    override var return_code: Int,
    override var return_msg: String,
    var prm_trde_trnsn: List<KiwoomIndexProgramTradeMinute>? = null // 프로그램매매추이
): KiwoomRes(return_code = return_code ,return_msg = return_msg)