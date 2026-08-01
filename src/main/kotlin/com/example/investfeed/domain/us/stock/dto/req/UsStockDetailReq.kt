package com.example.investfeed.domain.us.stock.dto.req

data class UsStockDetailReq(
    val stexTp: String = "", // 거래소구분 NA:AMEX, ND:NASDAQ, NY:NYSE
    val chartType: UsStockChartType = UsStockChartType.DAY,
)

enum class UsStockChartType(val ticScope: String?) {
    MINUTE_1("1"),
    MINUTE_3("3"),
    MINUTE_5("5"),
    MINUTE_10("10"),
    MINUTE_30("30"),
    DAY(null),
    WEEK(null),
    MONTH(null),
    YEAR(null),
}
