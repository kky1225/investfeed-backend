package com.example.investfeed.kiwoom.chart.dto.stock.res

/** 분·일·주·월·년봉 응답의 캔들 한 개를 같은 모양으로 맞춘 것 */
data class KiwoomStockChartRow(
    val dt: String?,            // 일자(yyyyMMdd) 또는 체결시간(yyyyMMddHHmmss)
    val cur_prc: String?,       // 종가
    val open_pric: String?,     // 시가
    val high_pric: String?,     // 고가
    val low_pric: String?,      // 저가
    val trde_qty: String?,      // 거래량
    val trde_prica: String?,    // 거래대금 (분봉은 없음)
)

/** 연속 조회 한 번의 결과. 최신→과거 순. nextKey 가 null 이면 더 받을 과거가 없다 */
data class KiwoomStockChartPage(
    val rows: List<KiwoomStockChartRow>,
    val nextKey: String?,
)
