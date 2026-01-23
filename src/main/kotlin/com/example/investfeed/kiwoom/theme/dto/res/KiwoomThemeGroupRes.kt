package com.example.investfeed.kiwoom.theme.dto.res

data class KiwoomThemeGroupRes(
    var return_code: Int, // 응답 코드
    var return_msg: String, // 응답 메세지
    var thema_grp: List<KiwoomThemeGroup>? = null // 테마그룹별
)