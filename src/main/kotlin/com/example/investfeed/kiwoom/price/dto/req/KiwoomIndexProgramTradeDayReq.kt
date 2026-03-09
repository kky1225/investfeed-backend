package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomIndexProgramTradeDayReq(
    var date: String? = null, // 날짜 YYYYMMDD (종료일기준 1년간 데이터만 조회가능)
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액, 2:수량
    var mrkt_tp: String? = null, // 시장구분 0:코스피 , 1:코스닥
    var stex_tp: String? = null, // 거래소구분 1:KRX, 2:NXT, 3:통합
)