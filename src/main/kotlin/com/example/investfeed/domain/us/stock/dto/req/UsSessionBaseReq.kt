package com.example.investfeed.domain.us.stock.dto.req

data class UsSessionBaseReq(
    var stkCds: List<String> = emptyList(), // 미국 티커 목록 (콤마 구분, 최대 MAX_SESSION_BASE_STOCKS)
)
