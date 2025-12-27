package com.example.investfeed.kiwoom.stock.dto.res

data class KiwoomSectCodeListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var list: List<KiwoomSectCodeList> // 업종코드리스트
)