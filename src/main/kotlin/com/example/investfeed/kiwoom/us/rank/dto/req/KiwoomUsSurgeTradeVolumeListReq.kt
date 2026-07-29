package com.example.investfeed.kiwoom.us.rank.dto.req

data class KiwoomUsSurgeTradeVolumeListReq(
    var stex_tp: String, // 거래소구분 0:전체, 1:NYSE, 2:NASDAQ, 3:AMEX
    var inds_cd: String, // 업종코드 usa10101 API 참고
    var tm: String, // x일평균대비 5일, 10일, 20일, 30일
    var stk_tp: String, // 종목구분 0:전체, 1:주식
    var stk_cnd: String, // 종목조건 0:전체, 1:증100%, 2:증50%
    var pric_cnd: String, // 가격조건 0:전체
    var trde_prica_cnd: String, // 거래대금조건 0:전체 (USD, 1만단위)
    var trde_qty_tp: String, // 거래량조건 0:전체, 10~500(1만단위) 이상
)
