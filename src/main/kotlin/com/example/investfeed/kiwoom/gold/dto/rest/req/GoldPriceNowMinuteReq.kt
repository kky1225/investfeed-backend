package com.example.investfeed.kiwoom.gold.dto.rest.req

class GoldPriceNowMinuteReq(
    var stk_cd: String, // 종목코드 M04020000 금 99.99_1kg, M04020100 미니금 99.99_100g
    var tic_scope: String = "1",
)