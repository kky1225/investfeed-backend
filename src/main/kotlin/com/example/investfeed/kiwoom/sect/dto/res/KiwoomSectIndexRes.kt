package com.example.investfeed.kiwoom.sect.dto.res

data class KiwoomSectIndexRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var all_inds_idex: List<KiwoomSectIndex> // 전업종지수
)