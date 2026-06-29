package com.example.investfeed.toss.holding.dto.res

data class TossHoldingRes(
    var result: TossHoldingResult? = null
)

data class TossHoldingResult(
    var items: List<TossHoldingItem>? = null // 보유 종목 목록
)

data class TossHoldingItem(
    var symbol: String? = null,                // 종목코드 (국내 6자리 / 미국 티커)
    var name: String? = null,                  // 종목명
    var marketCountry: String? = null,         // KR / US
    var currency: String? = null,              // KRW / USD
    var quantity: String? = null,              // 보유수량
    var lastPrice: String? = null,             // 현재가 (종목 통화 기준)
    var averagePurchasePrice: String? = null,  // 평균매입가 (종목 통화 기준)
    var marketValue: TossItemMarketValue? = null,
    var profitLoss: TossItemProfitLoss? = null,
    var dailyProfitLoss: TossItemDailyProfitLoss? = null
)

data class TossItemProfitLoss(
    var amount: String? = null,  // 평가손익 (종목 통화 기준)
    var rate: String? = null     // 수익률 (비율, 예: 0.1077 = 10.77%)
)

data class TossItemMarketValue(
    var amount: String? = null,          // 평가금액 (종목 통화 기준)
    var purchaseAmount: String? = null,  // 매입금액 (종목 통화 기준)
    var amountAfterCost: String? = null  // 비용차감 평가금액
)

data class TossItemDailyProfitLoss(
    var amount: String? = null,  // 당일 손익 (종목 통화 기준)
    var rate: String? = null     // 당일 손익률(%)
)
