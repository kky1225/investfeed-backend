package com.example.investfeed.kiwoom.chart.enum

enum class IndexChartType(
    val value: String? = null
) {
    MINUTE_1("1"),
    MINUTE_3("3"),
    MINUTE_5("5"),
    MINUTE_10("10"),
    MINUTE_30("30"),
    DAY,
    WEEK,
    MONTH,
    YEAR
}