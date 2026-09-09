package com.example.investfeed.domain.multiview.dto.req

data class MultiViewStockStreamReq(
    val items: List<String> = emptyList(), // 국내 종목코드
    val usItems: List<MultiViewUsStreamItem> = emptyList(), // 미국 종목
)
