package com.example.investfeed.domain.us.stock.dto.res

data class UsSessionBaseRes(
    val session: String, // DAY / PRE / AFTER
    val sessionLabel: String, // 데이마켓 / 프리마켓 / 애프터마켓
    val baseDate: String, // 기준 정규장 거래일 (미국 현지, yyyy-MM-dd)
    val bases: List<UsSessionBaseItem>,
)

data class UsSessionBaseItem(
    val stkCd: String, // 티커
    val regularClose: Double, // 기준 정규장 공식 종가 (USD)
)
