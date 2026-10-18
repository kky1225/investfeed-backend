package com.example.investfeed.domain.investor.dto.res

data class InvestorListRes(
    var investorList: List<InvestorListItem>? = null,
    var baseDate: String? = null,       // 기준 거래일 (yyyy-MM-dd)
    var provisional: Boolean = false,   // 장중 잠정치(ka10063)면 true, 확정치(ka10058)면 false
)
