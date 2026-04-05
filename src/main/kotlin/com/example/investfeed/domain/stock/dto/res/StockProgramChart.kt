package com.example.investfeed.domain.stock.dto.res

data class StockProgramChart(
    var tm: String? = null, // 시간
    var prmSellAmt: String? = null, // 프로그램매도금액
    var prmBuyAmt: String? = null, // 프로그램매수금액
    var prmNetprpsAmt: String? = null, // 프로그램순매수금액
)
