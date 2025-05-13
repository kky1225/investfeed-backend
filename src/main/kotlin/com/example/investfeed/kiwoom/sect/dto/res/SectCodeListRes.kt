package com.example.investfeed.kiwoom.sect.dto.res

data class SectCodeListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var list: List<SectCodeList> // 업종코드리스트
)