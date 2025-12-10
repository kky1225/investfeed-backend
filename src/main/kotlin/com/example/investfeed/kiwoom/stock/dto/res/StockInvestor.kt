package com.example.investfeed.kiwoom.stock.dto.res

data class StockInvestor(
    var dt: String? = null, // 일자
    var ind_invsr: String? = null, // 개인투자자
    var frgnr_invsr: String? = null, // 외국인투자자
    var orgn: String? = null, // 기관계
    var etc_fnnc: String? = null, // 기타금융
    var fnnc_invt: String? = null, // 금융투자
    var insrnc: String? = null, // 보험
    var invtrt: String? = null, // 투신
    var samo_fund: String? = null, // 사모펀드
    var penfnd_etc: String? = null, // 연기금등
    var bank: String? = null, // 은행
    var etc_corp: String? = null, // 기타법인
    var natfor: String? = null, // 내외국인
)