package com.example.investfeed.kiwoom.stock.entity.req

data class KiwoomSurgeTradeVolumeListReq(
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var sort_tp: String, // 정렬구분 1:급증량, 2:급증률, 3:급감량, 4:급감률
    var tm_tp: String, // 시간구분 1:분, 2:전일
    var trde_qty_tp: String, // 거래량구분 5:5천주이상, 10:만주이상, 50:5만주이상, 100:10만주이상, 200:20만주이상, 300:30만주이상, 500:50만주이상, 1000:백만주이상
    var tm: String? = null, // 시간 분 입력
    var stk_cnd: String, // 종목조건 0:전체조회, 1:관리종목제외, 3:우선주제외, 11:정리매매종목제외, 4:관리종목,우선주제외, 5:증100제외, 6:증100만보기, 13:증60만보기, 12:증50만보기, 7:증40만보기, 8:증30만보기, 9:증20만보기, 17:ETN제외, 14:ETF제외, 18:ETF+ETN제외, 15:스팩제외, 20:ETF+ETN+스팩제외
    var pric_tp: String, // 가격구분 0:전체조회, 2:5만원이상, 5:1만원이상, 6:5천원이상, 8:1천원이상, 9:10만원이상
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT 3.통합
)