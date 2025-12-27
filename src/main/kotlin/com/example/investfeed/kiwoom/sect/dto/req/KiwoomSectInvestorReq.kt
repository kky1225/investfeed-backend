package com.example.investfeed.kiwoom.sect.dto.req

data class KiwoomSectInvestorReq (
    var mrkt_tp: String, // 시장 구분 코스피:0, 코스닥:1
    var amt_qty_tp: String, // 금액수량 구분 금액:0, 수량:1
    var base_dt: String? = null, // 기준일자 YYYYMMDD
    var stex_tp: String // 거래소 구분 1:KRX, 2:NXT, 3:통합
)