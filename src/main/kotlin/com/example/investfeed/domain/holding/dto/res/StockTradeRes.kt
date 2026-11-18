package com.example.investfeed.domain.holding.dto.res

/** 한 종목의 내 매매(차트 표시용). 연동한 증권사의 체결을 모두 모은 것 */
data class StockTradeRes(
    val trades: List<StockTradeItem>,       // 체결. 오래된 순
    // 지금 보유 중인 증권사별 수량과 평단가. 연동 계좌는 증권사 응답에서, 수동 입력 계좌는 저장된 값에서 읽는다(수동은 국내 종목만)
    val holdings: List<StockTradeHolding>,
    val failedBrokers: List<String>,        // 조회에 실패해 위 목록에서 빠진 증권사 이름
)

data class StockTradeItem(
    val broker: String,     // 증권사 이름
    // 체결 시각. 차트 캔들의 시각과 같은 기준(국내는 한국 시각, 미국은 미국 동부 시각)이다.
    // yyyyMMddHHmmss, 증권사가 체결 시각을 주지 않으면(키움) yyyyMMdd
    val dt: String,
    val side: StockTradeSide,
    val quantity: Double,
    val price: Double,      // 체결 단가. 한 주문이 나뉘어 체결됐으면 평균 단가
)

enum class StockTradeSide { BUY, SELL }

data class StockTradeHolding(
    val broker: String,
    val quantity: Double,   // 소수점 주식이면 소수
    val avgPrice: Double,   // 종목 통화 기준 (국내 원, 미국 달러)
)
