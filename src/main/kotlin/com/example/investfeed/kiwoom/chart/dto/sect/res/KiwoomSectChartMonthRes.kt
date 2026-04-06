package com.example.investfeed.kiwoom.chart.dto.sect.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSectChartMonthRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var inds_cd: String? = null, // 업종코드
    var inds_mth_pole_qry: List<KiwoomSectChartMonth>? = null // 업종월봉조회
): KiwoomRes(return_code, return_msg)