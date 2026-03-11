package com.example.investfeed.domain.stock.dto.res

data class StockProgram(
    var dt: String? = null, // 일자
    var prmSellQty: String? = null, // 프로그램매도수량
    var prmBuyQty: String? = null, // 프로그램매수수량
    var prmNetprpsQty: String? = null, // 프로그램순매수수량
    var prmNetprpsQtyIrds: String? = null, // 프로그램순매수수량증감
)