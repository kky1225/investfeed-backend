package com.example.investfeed.domain.assistant.dto.factsheet

import java.time.LocalDate
import java.time.LocalDateTime

data class IndexQuote(
    val indexCode: String,        // KOSPI / KOSDAQ / NASDAQ / SP500
    val tradeDate: LocalDate,     // 국내=KST 일자, 미국=미국 거래일
    val current: Double,
    val prevClose: Double,
    val high: Double,
    val low: Double,
) {
    val currentRate: Double get() = (current / prevClose - 1) * 100
    val highRate: Double get() = (high / prevClose - 1) * 100
    val lowRate: Double get() = (low / prevClose - 1) * 100
}

enum class AlertTrigger { THRESHOLD, CB }
enum class AlertDirection { UP, DOWN }

/** 판정 결과 1건. 카드·이력에 그대로 쓰인다 */
data class IndexAlertFact(
    val quote: IndexQuote,
    val trigger: AlertTrigger,
    val direction: AlertDirection,
    val triggerRate: Double,          // 발동 근거 등락률 (고가·저가, 국내 CB 는 현재값)
    val stage: Int? = null,           // CB 단계
    val level: Double? = null,        // 급변 알림이 새로 넘은 가장 높은 단계 (3 / 5%). 헤드라인 "+3% 도달"
    val firedAt: LocalDateTime,
)

/** 카드 본문용 시장 데이터 — 발동 지수가 속한 시장만 (2026-09-17 결정) */
data class IndexAlertCardFact(
    val primary: IndexFact?,          // 코스피 / 코스닥 / 나스닥
    val secondary: IndexFact?,        // 코스피200 / 코스닥150 / S&P500
    val flow: MarketFlowFact?,        // 국내만. 코스피200·코스닥150 은 상위 지수 수급
)

data class ReleaseFact(
    val targetKey: String,
    val country: String,
    val eventDate: LocalDate,
    val eventName: String,
    val value: String,
    val prevValue: String?,
    val detectedAt: LocalDateTime,
)

data class HoldingAlertHit(
    val memberId: Long,
    val assetCode: String,                 // 005930_AL / TLT_US / KRW-BTC
    val assetName: String,
    val link: String,                      // 내부 라우트 (/stock/detail/… · /us-stock/detail/… · /crypto/detail/…)
    val direction: com.example.investfeed.domain.notification.entity.Direction,   // UP / DOWN / UPPER_LIMIT / LOWER_LIMIT / HIGH_52W / LOW_52W
    val threshold: Double,                 // 이번 분에 새로 넘은 가장 높은 임계(%). 상하한가·52주는 0
    val triggerRate: Double?,              // 판정 근거 등락률 (당일 고가·저가 기준). 52주는 null
    val price: Double?,                    // 알림함과 같은 괄호 금액: 등락·상하한가=발송 시점 현재가, 52주=52주 고가·저가 (국내·코인 원, 미국 달러)
)
