package com.example.investfeed.kiwoom.us.stock.dto.res

data class KiwoomUsStockInfoListItem(
    var stex_tp: String? = null, // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    var stk_cd: String? = null, // 종목코드 (티커)
    var stk_nm: String? = null, // 종목명
    var stk_enm: String? = null, // 종목영문명
    var mkgb: String? = null, // 거래소명
    var upgb: String? = null, // 업종명
    var isEtf: String? = null, // ETF 여부
)
