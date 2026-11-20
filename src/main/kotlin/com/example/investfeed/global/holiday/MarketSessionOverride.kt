package com.example.investfeed.global.holiday

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 국내 장 시각이 평소(09:00~15:30)와 다른 날 (수능일 등). 관리자가 캘린더 관리 화면에서 등록한다.
 * 새해 첫 거래일은 코드 규칙(KrMarketSessionService)으로 계산하고, 같은 날을 여기 등록하면 등록한 값이 우선한다.
 * 애프터마켓 종료(20:00)는 이런 날에도 바뀌지 않아 저장하지 않는다
 */
@Entity
@Table(
    name = "market_session_override",
    uniqueConstraints = [UniqueConstraint(columnNames = ["market", "trade_date"])]
)
class MarketSessionOverride(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 5)
    val market: String = HolidayService.MARKET_KR,

    @Column(name = "trade_date", nullable = false)
    var tradeDate: LocalDate,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "open_time", nullable = false)
    var openTime: LocalTime,

    @Column(name = "close_time", nullable = false)
    var closeTime: LocalTime,

    @Column(name = "pre_market", nullable = false)
    var preMarket: Boolean,   // NXT 프리마켓(08:00~08:50) 운영 여부

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
