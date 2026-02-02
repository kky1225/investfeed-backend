package com.example.investfeed.kiwoom.rank.dto.req

data class KiwoomInvestorTradeDailyReq(
    var trde_tp: String, // 매매구분 1:순매수, 2:순매도
    var mrkt_tp: String, // 시장구분 000:전체, 001:코스피, 101:코스닥
    var orgn_tp: String, // 기관구분 9000:외국인, 9100:외국계, 1000:금융투자, 3000:투신, 5000:기타금융, 4000:은행, 2000:보험, 6000:연기금, 7000:국가, 7100:기타법인, 9999:기관계
)