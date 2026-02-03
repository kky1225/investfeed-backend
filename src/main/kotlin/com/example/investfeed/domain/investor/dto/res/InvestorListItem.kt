package com.example.investfeed.domain.investor.dto.res

data class InvestorListItem(
    var stkCd: String? = null, // 종목코
    var stkNm: String? = null, // 종목명
    var selQty: String? = null, // 매도량
    var buyQty: String? = null, // 매수량
    var netslmt: String? = null, // 순매도
)