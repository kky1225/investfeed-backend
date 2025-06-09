package com.example.investfeed.kiwoom.sect.dto.rest.req

data class SectInvestorReq (
    var mrkt_tp: String, // 시장 구분
    var amt_qty_tp: String, // 금액수량 구분
    var base_dt: String, // 기준일자
    var stex_tp: String // 거래소 구분
)