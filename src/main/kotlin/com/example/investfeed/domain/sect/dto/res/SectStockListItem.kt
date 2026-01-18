package com.example.investfeed.domain.sect.dto.res

data class SectStockListItem(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가
    var predPreSig: String? = null, // 전일대비기호
    var nowTrdeQty: String? = null, // 현재거래량
)