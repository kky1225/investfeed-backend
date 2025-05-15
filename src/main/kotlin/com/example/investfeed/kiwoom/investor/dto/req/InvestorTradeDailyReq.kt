package com.example.investfeed.kiwoom.investor.dto.req

data class InvestorTradeDailyReq(
    var strt_dt: String, // 시작일자 YYYYMMDD
    var end_dt: String, // 종료일자 YYYYMMDD
    var trde_tp: String, // 매매구분 순매도:1, 순매수:2
    var mrkt_tp: String, // 시장구분 001:코스피, 101:코스닥
    var invsr_tp: String, // 투자자구분 8000:개인, 9000:외국인, 1000:금융투자, 3000:투신, 5000:기타금융, 4000:은행, 2000:보험, 6000:연기금, 7000:국가, 7100:기타법인, 9999:기관계
    var stex_tp: String // 거래소구분 1:KRX, 2:NXT 3.통합
)