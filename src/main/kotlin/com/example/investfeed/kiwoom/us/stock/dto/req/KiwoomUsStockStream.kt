package com.example.investfeed.kiwoom.us.stock.dto.req

data class KiwoomUsStockStream(
    var item: List<KiwoomUsStockStreamItem>? = null, // 실시간 등록 요소 Map구조 (종목코드, 거래소코드) ex) [{"jmcode":"NVDA","stex_tp":"ND"}]
    var type: List<String> // 실시간 항목 TR 명(F4,F5,FE,FT)
)
