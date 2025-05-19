package com.example.investfeed.kiwoom.theme.dto.req

data class ThemeGroupStockListReq(
    var date_tp: String? = null, // 날짜구분 1일 ~ 99일 날짜입력
    var thema_grp_cd: String, // 테마그룹코드 테마그룹코드 번호
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT 3.통합
)