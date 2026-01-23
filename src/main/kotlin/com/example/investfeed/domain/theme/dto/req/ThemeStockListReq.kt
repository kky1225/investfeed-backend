package com.example.investfeed.domain.theme.dto.req

data class ThemeStockListReq(
    var dateTp: String, // 날짜구분 1일 ~ 99일 날짜입력
    var themaGrpCd: String
)