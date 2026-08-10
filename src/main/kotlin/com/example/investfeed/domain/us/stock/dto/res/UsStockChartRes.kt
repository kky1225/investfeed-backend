package com.example.investfeed.domain.us.stock.dto.res

data class UsStockChartRes(
    val usStockInfo: UsStockInfo,
    val chartList: List<UsStockChart>,
)
