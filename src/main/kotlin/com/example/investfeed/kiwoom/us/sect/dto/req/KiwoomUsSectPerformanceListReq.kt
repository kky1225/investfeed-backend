package com.example.investfeed.kiwoom.us.sect.dto.req

data class KiwoomUsSectPerformanceListReq(
    var stex_tp: String, // 거래소구분 0:전체, 1:NYSE, 2:AMEX, 3:NASDAQ
    var inds_cd: String, // 업종코드 0:전체, usa10101 API 참고
)
