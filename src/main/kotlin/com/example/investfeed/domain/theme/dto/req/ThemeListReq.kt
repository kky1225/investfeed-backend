package com.example.investfeed.domain.theme.dto.req

data class ThemeListReq(
    var dateTp: String, // 날짜구분 n일전 (1일 ~ 99일 날짜입력)
    var fluPlAmtTp: String, // 등락수익구분 1:상위기간수익률, 2:하위기간수익률, 3:상위등락률, 4:하위등락률
)