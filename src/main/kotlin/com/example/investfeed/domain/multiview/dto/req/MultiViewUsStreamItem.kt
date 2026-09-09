package com.example.investfeed.domain.multiview.dto.req

data class MultiViewUsStreamItem(
    val stkCd: String, // 종목코드
    val stexTp: String, // 거래소구분 ND:NASDAQ, NY:NYSE, NA:AMEX
)
