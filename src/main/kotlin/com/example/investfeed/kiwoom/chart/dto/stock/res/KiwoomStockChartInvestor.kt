package com.example.investfeed.kiwoom.chart.dto.stock.res

data class KiwoomStockChartInvestor(
    var tm: String? = null, // 시간
    var frgnr_invsr: String? = null, // 외국인투자자
    var orgn: String? = null, // 기관계
    var invtrt: String? = null, // 투신
    var insrnc: String? = null, // 보험
    var bank: String? = null, // 은행
    var penfnd_etc: String? = null, // 연기금등
    var etc_corp: String? = null, // 기타법인
    var natn: String? = null, // 국가
)