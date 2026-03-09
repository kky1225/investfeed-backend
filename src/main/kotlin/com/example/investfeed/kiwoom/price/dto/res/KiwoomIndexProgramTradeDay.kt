package com.example.investfeed.kiwoom.price.dto.res

data class KiwoomIndexProgramTradeDay(
    var dt: String? = null, // 일자
    var kospi200: String? = null, // KOSPI200
    var basis: String? = null, // BASIS
    var dfrt_trde_tdy: String? = null, // 차익거래당일
    var dfrt_trde_acc: String? = null, // 차익거래누적
    var ndiffpro_trde_tdy: String? = null, // 비차익거래당일
    var ndiffpro_trde_acc: String? = null, // 비차익거래누적
    var all_tdy: String? = null, // 전체당일
    var all_acc: String? = null, // 전체누적
)