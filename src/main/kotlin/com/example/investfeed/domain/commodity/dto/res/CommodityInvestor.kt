package com.example.investfeed.domain.commodity.dto.res

data class CommodityInvestor(
    var dt: String? = null, // 일자
    var indInvsr: String? = null, // 개인투자자
    var frgnrInvsr: String? = null, // 외국인투자자
    var orgn: String? = null, // 기관계
    var etcFnnc: String? = null, // 기타금융
    var fnncInvt: String? = null, // 금융투자
    var insrnc: String? = null, // 보험
    var invtrt: String? = null, // 투신
    var samoFund: String? = null, // 사모펀드
    var penfndEtc: String? = null, // 연기금등
    var bank: String? = null, // 은행
    var etcCorp: String? = null, // 기타법인
    var natfor: String? = null, // 내외국인
)