package com.example.investfeed.kiwoom.price.dto.req

data class KiwoomInvestorTradeOpenMarketReq(
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var amt_qty_tp: String, // 금액수량구분 1: 금액&수량
    var invsr: String, // 투자자별 6:외국인, 7:기관계, 1:투신, 0:보험, 2:은행, 3:연기금, 4:국가, 5:기타법인
    var frgn_all: String, // 외국계전체 1:체크, 0:미체크
    var smtm_netprps_tp: String, // 동시순매수구분 1:체크, 0:미체크
    var stex_tp: String, // 거래소구분 1:KRX, 2:NXT 3.통합
)