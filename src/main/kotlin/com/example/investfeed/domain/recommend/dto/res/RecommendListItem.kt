package com.example.investfeed.domain.recommend.dto.res

data class RecommendListItem(
    var type: String? = null, // STRONG_BUY / BUY / HOLD / SELL / STRONG_SELL
    var stkCd: String? = null, // 종목코드
    var stkNm: String? = null, // 종목명
    var fluRt: String? = null, // 등락률
    var curPrc: String? = null, // 현재가
    var preSig: String? = null, // 대비기호
    var predPre: String? = null, // 전일대비
    var todayDirection: String? = null, // MATCH / MISMATCH / null (당일 매매 동향 보조 지표)
    var isHolding: Boolean = false, // 사용자가 키움증권에 보유 중인 종목인지
    var streakDays: Int = 0, // 같은 진영(매수/매도)으로 연속 추천된 일수
)
