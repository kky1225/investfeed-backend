package com.example.investfeed.domain.papertrade.admin.dto.res

/**
 * 모의계좌 실현손익 — 사용자 페이지 RealizedPnlTab 의 *월별 행 구조* 미러.
 * ka10074 일자별 응답을 (year, month)로 group-by 하여 월별 합계 행으로 변환.
 */
data class AdminPaperRealizedPnlRes(
    val viewMode: String,          // monthly / yearly / all
    val year: Int?,
    val month: Int?,
    val items: List<MonthlyItem>,
) {
    data class MonthlyItem(
        val year: Int,
        val month: Int,
        val realizedPnl: Long,
        val totalBuyAmt: Long,
        val totalSellAmt: Long,
        val tradeFee: Long,
        val tradeTax: Long,
    )
}
