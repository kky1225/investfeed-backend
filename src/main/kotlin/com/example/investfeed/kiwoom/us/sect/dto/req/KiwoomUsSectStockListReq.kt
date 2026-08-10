package com.example.investfeed.kiwoom.us.sect.dto.req

data class KiwoomUsSectStockListReq(
    var stex_tp: String, // 거래소구분 0:전체, 1:NY(NYSE), 2:NA(AMEX), 3:ND(NASDAQ)
    var sort_tp: String, // 정렬기준구분 1:등락율상위, 2:등락율하위
    var inds_cd: String, // 업종코드 000:전체(기본값), usa10101 API 참고
)
