package com.example.investfeed.kiwoom.stock.dto.res

data class KiwoomSectCodeList(
    var marketCode: String? = null, // 시장구분코드
    var code: String? = null, // 코드
    var name: String? = null, // 업종명
    var group: String? = null // 그룹
)