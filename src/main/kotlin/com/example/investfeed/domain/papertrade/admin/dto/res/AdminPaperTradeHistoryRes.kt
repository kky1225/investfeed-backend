package com.example.investfeed.domain.papertrade.admin.dto.res

/**
 * 모의계좌 거래내역 (kt00007 계좌별 주문체결내역 상세, qry_tp=4 체결내역만).
 * 단일 일자(`ordDt`) 기준 — 키움 명세가 일자별 조회 모델이라 그대로 따름.
 */
data class AdminPaperTradeHistoryRes(
    val ordDt: String,                // YYYYMMDD
    val items: List<TradeItem>,
) {
    data class TradeItem(
        val ordDt: String,            // 요청 일자(응답엔 없어 요청값 echo)
        val ordTm: String?,           // HH:MM:SS
        val stkCd: String,
        val stkNm: String,
        val ioTpNm: String?,          // 현금매수 / 현금매도 등 (구분)
        val trdeTp: String?,          // 매매구분 (시장가/보통)
        val cntrQty: Long,            // 체결수량
        val cntrUv: Long,             // 체결단가
        val ordQty: Long,             // 주문수량
        val ordUv: Long,              // 주문단가
        val ordNo: String?,
        val tradeReason: String?,     // 매매 사유 (paper_fill.note, 주문번호 매칭) — 등급/사이징가/회차, 현금회수·2차 발행 등
    )
}