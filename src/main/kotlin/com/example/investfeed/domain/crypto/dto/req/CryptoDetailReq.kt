package com.example.investfeed.domain.crypto.dto.req

data class CryptoDetailReq(
    val market: String,
    val chartType: CryptoChartType = CryptoChartType.DAY,
)

enum class CryptoChartType(val unit: Int?) {
    MINUTE_1(1),
    MINUTE_3(3),
    MINUTE_5(5),
    MINUTE_10(10),
    MINUTE_30(30),
    DAY(null),
    WEEK(null),
    MONTH(null),
    YEAR(null),
}
