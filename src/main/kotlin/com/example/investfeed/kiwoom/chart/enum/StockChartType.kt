package com.example.investfeed.kiwoom.chart.enum

enum class StockChartType(
    val value: String? = null
) {
    MINUTE_1("1"),
    MINUTE_3("3"),
    MINUTE_5("5"),
    MINUTE_10("10"),
    MINUTE_15("15"),
    MINUTE_30("30"),
    MINUTE_60("60"),
    DAY,
    WEEK,
    MONTH,
    YEAR
}