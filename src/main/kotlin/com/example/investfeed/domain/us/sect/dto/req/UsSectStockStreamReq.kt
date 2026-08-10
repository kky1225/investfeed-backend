package com.example.investfeed.domain.us.sect.dto.req

data class UsSectStockStreamReq(
    var items: List<UsSectStockStreamItem>, // 실시간 등록 종목 리스트
)
