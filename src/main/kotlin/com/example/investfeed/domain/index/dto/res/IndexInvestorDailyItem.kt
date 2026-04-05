package com.example.investfeed.domain.index.dto.res

data class IndexInvestorDailyItem(
    var dt: String? = null, // 일자
    var indNetprps: String? = null, // 개인순매수
    var frgnrNetprps: String? = null, // 외국인순매수
    var orgnNetprps: String? = null, // 기관계순매수
    var scNetprps: String? = null, // 증권순매수
    var insrncNetprps: String? = null, // 보험순매수
    var invtrtNetprps: String? = null, // 투신순매수
    var bankNetprps: String? = null, // 은행순매수
    var endwNetprps: String? = null, // 기금순매수
    var etcCorpNetprps: String? = null, // 기타법인순매수
    var samoFundNetprps: String? = null, // 사모펀드순매수
    var natnNetprps: String? = null, // 국가순매수
)
