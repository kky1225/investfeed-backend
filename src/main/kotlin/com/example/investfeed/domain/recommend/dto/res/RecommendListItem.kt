package com.example.investfeed.domain.recommend.dto.res

data class RecommendListItem(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가
    var preSig: String? = null, // 대비기호
    var predPre: String? = null, // 전일대비
)