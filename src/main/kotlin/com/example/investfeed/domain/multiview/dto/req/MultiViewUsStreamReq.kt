package com.example.investfeed.domain.multiview.dto.req

data class MultiViewUsStreamReq(
    val items: List<MultiViewUsStreamItem>
)

data class MultiViewUsStreamItem(
    val stkCd: String, // 종목코드 (티커)
    val stexTp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
)
