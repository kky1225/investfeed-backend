package com.example.investfeed.kiwoom.sect.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomSectIndexRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var all_inds_idex: List<KiwoomSectIndex>? = null // 전업종지수
): KiwoomRes(return_code, return_msg)