package com.example.investfeed.kiwoom.chart.dto.sect.res

data class KiwoomSectChartMinuteRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var inds_cd: String? = null, // 업종코드
    var inds_min_pole_qry: List<KiwoomSectChartMinute>? = null // 업종분봉조회
)