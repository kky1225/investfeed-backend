package com.example.investfeed.domain.theme.dto.res

data class ThemeStockListItem(
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var curPrc: String? = null, // 현재가
    var fluSig: String? = null, // 등락기호 1: 상한가, 2:상승, 3:보합, 4:하한가, 5:하락
    var predPre: String? = null, // 전일대비
    var fluRt: String? = null, // 등락율
    var accTrdeQty: String? = null, // 누적거래량
    var dtPrftRtN: String? = null, // 기간수익률n
)