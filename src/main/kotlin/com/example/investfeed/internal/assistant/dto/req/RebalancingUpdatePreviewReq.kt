package com.example.investfeed.internal.assistant.dto.req

/** 말한 항목만 온다 — 빠진 항목은 기존 설정을 그대로 쓴다. 방향은 MIN(이상)·MAX(이하), 값 검증은 ActionToolService */
data class RebalancingUpdatePreviewReq(
    val stockRatio: Int? = null,
    val stockDirection: String? = null,
    val cryptoRatio: Int? = null,
    val cryptoDirection: String? = null,
    val cashRatio: Int? = null,
    val cashDirection: String? = null,
    val maxStockRatio: Int? = null,
) {
    fun isEmpty() = listOf(stockRatio, stockDirection, cryptoRatio, cryptoDirection, cashRatio, cashDirection, maxStockRatio).all { it == null }
}
