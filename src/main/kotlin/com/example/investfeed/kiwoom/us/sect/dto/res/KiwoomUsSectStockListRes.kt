package com.example.investfeed.kiwoom.us.sect.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsSectStockListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var result_list: List<KiwoomUsSectStockRes>? = null // 업종별 등락률 상위/하위
): KiwoomRes(return_code, return_msg)
