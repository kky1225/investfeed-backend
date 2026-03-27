package com.example.investfeed.domain.rank.dto.res

data class RankListItem(
    var stkCd: String? = null, // 종목코드
    var rank: String? = null, // 현재순위
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가
    var trdePrica: String? = null, // 거래대금
    var nxtEnable: String? = null, // NXT가능여부
)