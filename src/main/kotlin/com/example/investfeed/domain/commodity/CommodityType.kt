package com.example.investfeed.domain.commodity

enum class CommodityType(
    val stkCd: String,
    val stkNm: String,
) {
    GOLD(stkCd = "M04020000", stkNm = "금 현물"),
    MINI_GOLD(stkCd = "M04020100", stkNm = "미니 금 현물"),
}