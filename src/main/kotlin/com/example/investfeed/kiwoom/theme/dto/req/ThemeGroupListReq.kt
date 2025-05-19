package com.example.investfeed.kiwoom.theme.dto.req

data class ThemeGroupListReq(
    var qry_tp: String, // 검색구분 0:전체검색, 1:테마검색, 2:종목검색
    var stk_cd: String? = null, // 종목코드 검색하려는 종목코드
    var date_tp: String, // 날짜구분 n일전 (1일 ~ 99일 날짜입력)
    var thema_nm: String? = null, // 테마명 검색하려는 테마명
    var flu_pl_amt_tp: String, // 등락수익구분 1:상위기간수익률, 2:하위기간수익률, 3:상위등락률, 4:하위등락률
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT 3.통합
)