package com.example.investfeed.kiwoom.us.sect.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomUsSectPerformanceListRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var result_list: List<KiwoomUsSectPerformanceRes>? = null // 업종별 기간별 수익률
): KiwoomRes(return_code, return_msg)
