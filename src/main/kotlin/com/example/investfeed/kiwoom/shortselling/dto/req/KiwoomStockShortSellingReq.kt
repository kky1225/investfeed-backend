package com.example.investfeed.kiwoom.shortselling.dto.req

data class KiwoomStockShortSellingReq(
    var stk_cd: String? = null, // 종목코드 거래소별 종목코드(KRX:039490,NXT:039490_NX,SOR:039490_AL)
    var tm_tp: String? = null, // 시간구분 0:시작일, 1:기간
    var strt_dt: String? = null, // 시작일자 YYYYMMDD
    var end_dt: String? = null, // 종료일자 YYYYMMDD

)