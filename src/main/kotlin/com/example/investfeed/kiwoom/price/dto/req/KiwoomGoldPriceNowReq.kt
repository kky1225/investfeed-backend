package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomGoldPriceNowReq (
    var stk_cd: String // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
)