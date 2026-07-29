package com.example.investfeed.domain.us.rank.dto.req

data class UsStockStreamReq(
    var items: List<UsStockStreamItem>, // 실시간 등록 종목 리스트
)
