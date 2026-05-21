package com.example.investfeed.domain.rebalancing.dto.res

data class OverweightStockItem(
    val stkCd: String,
    val stkNm: String,
    val brokerName: String,
    val currentRatio: Double,
    val maxRatio: Int,
    val curPrc: Long,
    val evltAmt: Long,
    val sellQuantity: Long,
    val sellAmount: Long
)