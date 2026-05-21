package com.example.investfeed.domain.papertrade.admin.dto.res

import java.time.LocalDate

data class AdminHoldingGradeRes(
    val evalDate: LocalDate?,
    val items: List<HoldingGradeItem>,
) {
    data class HoldingGradeItem(
        val stkCd: String,
        val stkNm: String,
        val type: String,             // STRONG_BUY / BUY / HOLD / SELL / STRONG_SELL
        val originSide: String?,      // BUY / SELL
        val marketType: String?,      // KOSPI / KOSDAQ
        val penfndK: Double?,
        val frgnrMcapRatio: Double?,
    )
}
