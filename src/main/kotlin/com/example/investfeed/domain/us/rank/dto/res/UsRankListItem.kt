package com.example.investfeed.domain.us.rank.dto.res

data class UsRankListItem(
    var stkCd: String? = null, // 종목코드 (티커)
    var stexTp: String? = null, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
    var rank: String? = null, // 현재순위
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가 (USD)
    var trdePrica: String? = null, // 거래대금/거래량/급증률 (탭별 값)
)