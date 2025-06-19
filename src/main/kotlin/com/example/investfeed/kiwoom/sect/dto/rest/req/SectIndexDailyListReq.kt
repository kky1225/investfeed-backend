package com.example.investfeed.kiwoom.sect.dto.rest.req

data class SectIndexDailyListReq(
    var mrkt_tp: String, // 시장구분 0:코스피, 1:코스닥, 2:코스피200
    var inds_cd: String // 업종코드 001:종합(KOSPI), 002:대형주, 003:중형주, 004:소형주 101:종합(KOSDAQ), 201:KOSPI200, 302:KOSTAR, 701: KRX100 나머지 ※ 업종코드 참고
)