package com.example.investfeed.kiwoom.theme.dto.res

import com.example.investfeed.kiwoom.KiwoomRes

data class KiwoomThemeGroupStockRes(
    override var return_code: Int, // 응답 코드
    override var return_msg: String, // 응답 메세지
    var flu_rt: String? = null, // 등락률
    var dt_prft_rt: String? = null, // 기간수익률
    var thema_comp_stk: List<KiwoomThemeGroupStock>? = null // 테마구성종목
): KiwoomRes(return_code, return_msg)