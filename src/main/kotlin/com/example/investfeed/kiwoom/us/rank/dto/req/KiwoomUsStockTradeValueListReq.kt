package com.example.investfeed.kiwoom.us.rank.dto.req

data class KiwoomUsStockTradeValueListReq(
    var stex_tp: String, // 거래소구분 0:전체, 1:NYSE, 2:NASDAQ, 3:AMEX
    var inds_cd: String, // 업종코드 000:전체, usa10101 API 참고
    var stk_tp: String, // 종목구분 0:전체, 1:주식
    var trde_qty_tp: String, // 거래량조건 0:전체, 10~500(1만단위) 이상
    var stk_cnd: String, // 종목필터구분 0:전체, 1:증100%만보기, 2:증50%만보기
    var pric_cnd: String, // 검색가격대구분 0:전체
    var trde_prica_cnd: String, // 거래대금조건 0:전체 (USD, 1만단위)
)
