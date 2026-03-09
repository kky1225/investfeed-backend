package com.example.investfeed.kiwoom.price.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomIndexProgramTradeDayRes(
    override var return_code: Int,
    override var return_msg: String,
    var prm_trde_acc_trnsn: List<KiwoomIndexProgramTradeDay>? = null // 프로그램매매누적추이
): KiwoomRes(return_code = return_code ,return_msg = return_msg)