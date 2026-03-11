package com.example.investfeed.domain.stock.dto.res

data class StockInvestorChart(
    var tm: String? = null, // 시간
    var frgnrInvsr: String? = null, // 외국인투자자
    var orgn: String? = null, // 기관계
    var penfnd_etc: String? = null, // 연기금등
)