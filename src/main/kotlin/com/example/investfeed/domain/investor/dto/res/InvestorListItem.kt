package com.example.investfeed.domain.investor.dto.res

data class InvestorListItem(
    var forNetslmtStkCd: String? = null,
    var forNetslmtStkNm: String? = null,
    var forNetslmtAmt: String? = null,
    var forNetslmtQty: String? = null,
    var forNetprpsStkCd: String? = null,
    var forNetprpsStkNm: String? = null,
    var forNetprpsAmt: String? = null,
    var forNetprpsQty: String? = null,

    var orgnNetslmtStkCd: String? = null,
    var orgnNetslmtStkNm: String? = null,
    var orgnNetslmtAmt: String? = null,
    var orgnNetslmtQty: String? = null,
    var orgnNetprpsStkCd: String? = null,
    var orgnNetprpsStkNm: String? = null,
    var orgnNetprpsAmt: String? = null,
    var orgnNetprpsQty: String? = null,


//    var stkCd: String? = null, // 종목코
//    var stkNm: String? = null, // 종목명
//    var selQty: String? = null, // 매도량
//    var buyQty: String? = null, // 매수량
//    var netslmt: String? = null, // 순매도
)