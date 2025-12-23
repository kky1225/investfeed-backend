package com.example.investfeed.domain.stock.dto.req

data class StockNewPriceListReq(
    var mrkt_tp: String, // 000:전체, 001:코스피, 101:코스닥
    var ntl_tp: String, // 1:신고가,2:신저가
    var high_low_close_tp: String, // 1:고저기준, 2:종가기준
    var stk_cnd: String, // 0:전체조회,1:관리종목제외, 3:우선주제외, 5:증100제외, 6:증100만보기, 7:증40만보기, 8:증30만보기
    var trde_qty_tp: String, // 00000:전체조회, 00010:만주이상, 00050:5만주이상, 00100:10만주이상, 00150:15만주이상, 00200:20만주이상, 00300:30만주이상, 00500:50만주이상, 01000:백만주이상
    var crd_cnd: String, // 0:전체조회, 1:신용융자A군, 2:신용융자B군, 3:신용융자C군, 4:신용융자D군, 9:신용융자전체
    var updown_incls: String, // 0:미포함, 1:포함
    var dt: String, // 5:5일, 10:10일, 20:20일, 60:60일, 250:250일, 250일까지 입력가능
    var stex_tp: String // 1:KRX, 2:NXT 3.통합
)