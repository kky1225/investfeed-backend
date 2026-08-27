package com.example.investfeed.domain.papertrade.admin.dto.res

data class AdminPaperAccountRes(
    val summary: AccountSummary,
    val holdings: List<HoldingItem>,
) {
    data class AccountSummary(
        val deposit: Long,            // 예수금 (entr)
        val orderableAmt: Long,       // 주문가능금액 (ord_alow_amt)
        val totalPurAmt: Long,        // 총매입금액
        val totalEvltAmt: Long,       // 총평가금액
        val totalEvltPl: Long,        // 총평가손익
        val totalPrftRt: Double?,     // 총수익률(%)
        val nav: Long,                // 총 자산 (orderableAmt + totalEvltAmt)
    )

    data class HoldingItem(
        val stkCd: String,
        val stkNm: String,
        val rmndQty: Long,            // 보유수량
        val trdeAbleQty: Long,        // 매매가능수량
        val purPric: Long,            // 매입가
        val curPrc: Long,             // 현재가
        val purAmt: Long,             // 매입금액
        val evltAmt: Long,            // 평가금액
        val evltvPrft: Long,          // 평가손익
        val prftRt: Double?,          // 수익률(%)
        val possRt: Double?,          // 보유비중(%)
    )
}
