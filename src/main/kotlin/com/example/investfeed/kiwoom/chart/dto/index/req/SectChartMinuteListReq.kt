package com.example.investfeed.kiwoom.chart.dto.index.req

data class SectChartMinuteListReq(
    var inds_cd: String, // 업종코드 001:종합(KOSPI), 002:대형주, 003:중형주, 004:소형주 101:종합(KOSDAQ), 201:KOSPI200, 302:KOSTAR, 701: KRX100 나머지 ※ 업종코드 참고
    var tic_scope: String // 틱범위 1:1틱, 3:3틱, 5:5틱, 10:10틱, 30:30틱
)