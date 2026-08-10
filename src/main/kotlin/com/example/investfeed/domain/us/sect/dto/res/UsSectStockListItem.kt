package com.example.investfeed.domain.us.sect.dto.res

data class UsSectStockListItem(
    var stkCd: String? = null, // 종목코드
    var stexTp: String? = null, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가 (USD)
    var predPreSig: String? = null, // 전일대비기호
    var accTrdeQty: String? = null, // 거래량
)
