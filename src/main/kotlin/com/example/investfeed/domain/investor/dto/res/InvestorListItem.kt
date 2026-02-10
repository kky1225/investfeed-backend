package com.example.investfeed.domain.investor.dto.res

data class InvestorListItem(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var curPrc: String? = null, // 현재가
    var preSig: String? = null, // 대비기호
    var predPre: String? = null, // 전일대비
    var fluRt: String? = null, // 등락율
    var accTrdeQty: String? = null, // 누적거래량
    var netprpsAmt: String? = null, // 순매수금액
    var buyAmt: String? = null, // 매수금액
    var sellAmt: String? = null, // 매도금액
    var netprpsQty: String? = null, // 순매수수량
    var buyQty: String? = null, // 매수수량
    var sellQty: String? = null, // 매도수량
)