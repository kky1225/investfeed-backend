package com.example.investfeed.kiwoom.gold.dto.rest.req

data class GoldPriceNowReq (
    var stk_cd: String = "M04020000" // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
)