package com.example.investfeed.toss.exchangerate.dto.res

data class TossExchangeRateRes(
    var result: TossExchangeRate? = null
)

data class TossExchangeRate(
    var baseCurrency: String? = null, // 기준 통화 (USD)
    var quoteCurrency: String? = null, // 상대 통화 (KRW)
    var rate: String? = null, // 매수 환율 — midRate 에 증권사 마진이 얹힌 값
    var midRate: String? = null, // 매매기준율(은행간 mid rate). 스프레드가 없어 자산 평가 환산에 쓴다
    var basisPoint: String? = null, // (rate - midRate) / midRate * 10000
    var rateChangeType: String? = null, // 등락 구분 UP/EQUAL/DOWN
    var validFrom: String? = null, // 환율 유효 시작 시각
    var validUntil: String? = null, // 환율 유효 종료 시각
)
