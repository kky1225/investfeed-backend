package com.example.investfeed.kiwoom.theme.dto.res

data class ThemeGroupList(
    var thema_grp_cd: String? = null, // 테마그룹코드
    var thema_nm: String? = null, // 테마명
    var stk_num: String? = null, // 종목수
    var flu_sig: String? = null, // 등락기호
    var flu_rt: String? = null, // 등락율
    var rising_stk_num: String? = null, // 상승종목수
    var fall_stk_num: String? = null, // 하락종목수
    var dt_prft_rt: String? = null, // 기간수익률
    var main_stk: String? = null // 주요종목
)