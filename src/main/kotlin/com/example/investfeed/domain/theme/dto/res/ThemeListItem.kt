package com.example.investfeed.domain.theme.dto.res

data class ThemeListItem(
    var themaGrpCd: String? = null, // 테마그룹코드
    var themaNm: String? = null, // 테마명
    var fluSig: String? = null, // 등락기호
    var fluRt: String? = null, // 등락율
    var risingStkNum: String? = null, // 상승종목수
    var fallStkNum: String? = null, // 하락종목수
    var dtPrftRt: String? = null, // 기간수익률
    var mainStk: String? = null, // 주요종목
)