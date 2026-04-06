package com.example.investfeed.kiwoom.theme.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomThemeGroupRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var thema_grp: List<KiwoomThemeGroup>? = null // 테마그룹별
): KiwoomRes(return_code, return_msg)