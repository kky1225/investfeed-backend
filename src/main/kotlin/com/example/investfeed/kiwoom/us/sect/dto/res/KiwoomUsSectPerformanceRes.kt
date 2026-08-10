package com.example.investfeed.kiwoom.us.sect.dto.res

data class KiwoomUsSectPerformanceRes(
    var inds_cd: String? = null, // 업종코드
    var inds_nm: String? = null, // 업종명
    var perf_1d: String? = null, // 1일 수익률 (%)
    var perf_5d: String? = null, // 5일 수익률 (%)
    var perf_1m: String? = null, // 1개월 수익률 (%)
    var perf_3m: String? = null, // 3개월 수익률 (%)
    var perf_6m: String? = null, // 6개월 수익률 (%)
    var perf_ytd: String? = null, // 연중 수익률 (%)
    var perf_1y: String? = null, // 1년 수익률 (%)
)
