package com.example.investfeed.domain.stock.dto.req

data class StockJumpListReq (
    var mrkt_tp: String? = null, // 시장구분 000:전체, 001:코스피, 101:코스닥, 201:코스피200
    var flu_tp: String? = null, // 등락구분 1:급등, 2:급락
    var tm_tp: String? = null, // 시간구분 1:분전, 2:일전
    var tm: String? = null, // 시간 분 혹은 일입력
    var trde_qty_tp: String? = null, // 거래량구분 00000:전체조회, 00010:만주이상, 00050:5만주이상, 00100:10만주이상, 00150:15만주이상, 00200:20만주이상, 00300:30만주이상, 00500:50만주이상, 01000:백만주이상
    var stk_cnd: String? = null, // 종목조건 0:전체조회,1:관리종목제외, 3:우선주제외, 5:증100제외, 6:증100만보기, 7:증40만보기, 8:증30만보기
    var crd_cnd: String? = null, // 신용조건 0:전체조회, 1:신용융자A군, 2:신용융자B군, 3:신용융자C군, 4:신용융자D군, 9:신용융자전체
    var pric_cnd: String? = null, // 가격조건 0:전체조회, 1:1천원미만, 2:1천원~2천원, 3:2천원~3천원, 4:5천원~1만원, 5:1만원이상, 8:1천원이상
    var updown_incls: String? = null, // 상하한포함 0:미포함, 1:포함
    var stex_tp: String? = null // 거래소구분 1:KRX, 2:NXT 3.통합
)