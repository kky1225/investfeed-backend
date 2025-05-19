package com.example.investfeed.kiwoom.theme.dto.res

data class ThemeGroupListRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var thema_grp: List<ThemeGroupList>? = null // 테마그룹별
)