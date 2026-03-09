package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomIndexProgramTradeMinuteReq(
    var date: String? = null, // 날짜 YYYYMMDD
    var amt_qty_tp: String? = null, // 금액수량구분 1:금액(백만원), 2:수량(천주)
    var mrkt_tp: String? = null, // 시장구분 코스피- 거래소구분값 1일경우:P00101, 2일경우:P001_NX01, 3일경우:P001_AL01 코스닥- 거래소구분값 1일경우:P10102, 2일경우:P101_NX02, 3일경우:P101_AL02
    var min_tic_tp: String? = null, // 분틱구분 0:틱, 1:분
    var stex_tp: String? = null, // 거래소구분 1:KRX, 2:NXT 3.통합
)