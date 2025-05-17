package com.example.investfeed.kiwoom.rank.dto.req

data class RankTradeDailyVolumeListReq(
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var sort_tp: String, // 정렬구분 1:거래량, 2:거래회전율, 3:거래대금
    var mang_stk_incls: String, // 관리종목포함 0:관리종목 포함, 1:관리종목 미포함, 3:우선주제외, 11:정리매매종목제외, 4:관리종목, 우선주제외, 5:증100제외, 6:증100마나보기, 13:증60만보기, 12:증50만보기, 7:증40만보기, 8:증30만보기, 9:증20만보기, 14:ETF제외, 15:스팩제외, 16:ETF+ETN제외
    var crd_tp: String, // 신용구분 0:전체조회, 9:신용융자전체, 1:신용융자A군, 2:신용융자B군, 3:신용융자C군, 4:신용융자D군, 8:신용대주
    var trde_qty_tp: String, // 거래량구분 0:전체조회, 5:5천주이상, 10:1만주이상, 50:5만주이상, 100:10만주이상, 200:20만주이상, 300:30만주이상, 500:500만주이상, 1000:백만주이상
    var pric_tp: String, // 가격구분 0:전체조회, 1:1천원미만, 2:1천원이상, 3:1천원~2천원, 4:2천원~5천원, 5:5천원이상, 6:5천원~1만원, 10:1만원미만, 7:1만원이상, 8:5만원이상, 9:10만원이상
    var trde_prica_tp: String, // 거래대금구분
    var mrkt_open_tp: String, // 장운영구분 0:전체조회, 1:장중, 2:장전시간외, 3:장후시간외
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT 3.통합
)