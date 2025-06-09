package com.example.investfeed.kiwoom.sect.dto.rest.res

data class SectIndexListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var all_inds_idex: List<SectIndexList> // 전업종지수
)