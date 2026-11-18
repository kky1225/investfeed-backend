package com.example.investfeed.domain.holding.dto.req

data class StockTradeReq(
    val market: StockTradeMarket = StockTradeMarket.KR,
)

/** 내 매매를 조회할 종목의 시장. 체결 시각을 이 시장의 차트 시각(한국 / 미국 동부)으로 맞춰 준다 */
enum class StockTradeMarket { KR, US }
