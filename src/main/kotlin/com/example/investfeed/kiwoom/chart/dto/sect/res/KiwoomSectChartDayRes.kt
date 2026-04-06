package com.example.investfeed.kiwoom.chart.dto.sect.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSectChartDayRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var inds_cd: String? = null, // 업종코드
    var inds_dt_pole_qry: List<KiwoomSectChartDay>? = null, // 업종일봉조회
): KiwoomRes(return_code, return_msg)