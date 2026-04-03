package com.example.investfeed.domain.holding.dto.res

data class HoldingListRes(
    val totPurAmt: String, // 총매입금액
    val totEvltAmt: String, // 총평가금액
    val totEvltPl: String, // 총평가손익금액
    val totPrftRt: String, // 총수익률(%)
    val holdingList: List<HoldingItem>
)

data class HoldingItem(
    val id: Long = 0, // MemberHolding ID (DnD 정렬용)
    val stkCd: String, // 종목번호
    val stkNm: String, // 종목명
    val curPrc: String, // 현재가
    val purPric: String, // 매입가
    val purAmt: String, // 매입금액
    val evltAmt: String, // 평가금액
    val evltvPrft: String, // 평가손익
    val prftRt: String, // 수익률(%)
    val rmndQty: String, // 보유수량
    val possRt: String, // 보유비중(%)
    val predClosePric: String, // 전일종가
)
