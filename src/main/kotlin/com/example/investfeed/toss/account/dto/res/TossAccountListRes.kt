package com.example.investfeed.toss.account.dto.res

data class TossAccountListRes(
    var result: List<TossAccount>? = null
)

data class TossAccount(
    var accountNo: String? = null,   // 계좌번호
    var accountSeq: Long? = null,    // 계좌 식별자 (X-Tossinvest-Account 헤더 값)
    var accountType: String? = null  // BROKERAGE / OVERSEAS_DERIVATIVES / PENSION_SAVINGS / RESHORING_INVESTMENT
)
