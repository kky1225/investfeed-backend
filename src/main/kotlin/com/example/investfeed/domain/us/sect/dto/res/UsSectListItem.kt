package com.example.investfeed.domain.us.sect.dto.res

data class UsSectListItem(
    var indsCd: String? = null, // 업종코드
    var indsNm: String? = null, // 업종명
    var perf1d: String? = null, // 1일 수익률
    var perf5d: String? = null, // 5일 수익률
    var perf1m: String? = null, // 1개월 수익률
    var perf3m: String? = null, // 3개월 수익률
    var perf6m: String? = null, // 6개월 수익률
    var perfYtd: String? = null, // 연중 수익률
    var perf1y: String? = null, // 1년 수익률
)
