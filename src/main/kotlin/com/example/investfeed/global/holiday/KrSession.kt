package com.example.investfeed.global.holiday

import com.example.investfeed.common.util.MarketTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * 하루의 국내 장 시각. 날짜마다 다른 건 개장·마감·프리마켓 운영 여부뿐이고, 나머지 시각은 여기서 계산한다.
 * 휴장 여부는 담지 않는다 (HolidayService.isHoliday 로 따로 판정).
 * 만드는 곳은 KrMarketSessionService.of(date) — 등록된 날(DB) → 새해 첫 거래일(코드 규칙) → 평소 순서
 *
 * 시각 예시 (평소 / 수능일 10:00~16:30·프리마켓 휴장)
 *  - 정규장           09:00~15:30 / 10:00~16:30
 *  - NXT              08:00~20:00 / 10:00~20:00
 *  - 동시호가(개장 전) 08:50~09:00 / 09:50~10:00
 *  - 동시호가(마감 전) 15:20~15:30 / 16:20~16:30
 */
data class KrSession(
    val date: LocalDate,
    val open: LocalTime,        // KRX 정규장 개장
    val close: LocalTime,       // KRX 정규장 마감
    val preMarket: Boolean,     // NXT 프리마켓(08:00~08:50) 운영 여부. 수능일·새해 첫 거래일은 휴장
    val name: String? = null,   // 평소와 다른 날의 이름 (수능일·새해 첫 거래일). 평소면 null
) {
    companion object {
        val DEFAULT_OPEN: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_CLOSE: LocalTime = LocalTime.of(15, 30)
        val NXT_PRE_OPEN: LocalTime = LocalTime.of(8, 0)
        private val CALL_AUCTION: Duration = Duration.ofMinutes(10)        // 동시호가 (개장 전·마감 전)
        private val HOLDING_CLOSE_DELAY: Duration = Duration.ofMinutes(10) // 마감 후 보유 조회를 NXT 기준으로 바꾸는 시점
        private val CLOSE_SETTLE_DELAY: Duration = Duration.ofMinutes(30)  // 마감 데이터 정리 (매크로 마감 수집·마감 브리핑 기준)
        private val STOCK_ALERT_DELAY: Duration = Duration.ofMinutes(1)

        /** 평소 날 — 09:00~15:30, 프리마켓 운영 */
        fun default(date: LocalDate) = KrSession(date, DEFAULT_OPEN, DEFAULT_CLOSE, preMarket = true)
    }

    /** 평소와 다른 날인지 (수능일·새해 첫 거래일). 경제 캘린더에 "수능일 (10:00 개장·16:30 마감)" 으로 표시할 날을 고를 때 씀 */
    val isSpecial: Boolean get() = name != null

    /** NXT 거래 시작 — 프리마켓이 있으면 08:00, 없는 날(수능일 등)은 정규장 개장과 함께 시작 */
    val nxtOpen: LocalTime get() = if (preMarket) NXT_PRE_OPEN else open

    /** NXT 거래 종료(애프터마켓 끝) — 장 시각이 바뀌는 날에도 20:00 그대로 */
    val nxtClose: LocalTime get() = MarketTimeUtil.NXT_CLOSE

    /** 개장 전 동시호가 시작 — 개장 10분 전 (평소 08:50) */
    val openCallStart: LocalTime get() = open.minus(CALL_AUCTION)

    /** 마감 전 동시호가 시작 — 마감 10분 전 (평소 15:20) */
    val closeCallStart: LocalTime get() = close.minus(CALL_AUCTION)

    /** 보유 종목 조회를 KRX → NXT 기준으로 바꾸는 시각 — 마감 10분 후 (평소 15:40) */
    val holdingClose: LocalTime get() = close.plus(HOLDING_CLOSE_DELAY)

    /** 마감 데이터 정리가 끝났다고 보는 시각 — 마감 30분 후 (평소 16:00). 매크로 마감 수집·마감 브리핑 귀속일 기준 */
    val closeSettled: LocalTime get() = close.plus(CLOSE_SETTLE_DELAY)

    /** 평소 대비 개장이 밀린 시간 (수능일 1시간). 개장 기준 하루 한 번 작업(모의투자 1차·2차)을 이만큼 늦춘다 */
    val openShift: Duration get() = Duration.between(DEFAULT_OPEN, open)

    /** 평소 대비 마감이 밀린 시간 (수능일 1시간, 새해 첫 거래일 0). 마감 기준 하루 한 번 작업(마감 브리핑)을 이만큼 늦춘다 */
    val closeShift: Duration get() = Duration.between(DEFAULT_CLOSE, close)

    /** KRX 정규장 중인지 (개장 ≤ t < 마감). 화면 장 상태(TimeService)·KRX 실시간 연결(KiwoomStreamClient)에서 씀 */
    fun isKrxOpen(t: LocalTime): Boolean = !t.isBefore(open) && t.isBefore(close)

    /** NXT 거래 중인지 (NXT 시작 ≤ t < 20:00). NXT 실시간 연결(KiwoomStreamClient)에서 씀 */
    fun isNxtOpen(t: LocalTime): Boolean = !t.isBefore(nxtOpen) && t.isBefore(nxtClose)

    /**
     * 동시호가 시간인지 — 개장 전 10분 또는 마감 전 10분 (평소 08:50~09:00, 15:20~15:30).
     * 이 시간엔 주문이 모였다가 끝에 한 가격으로 체결되고, 거래소가 예상 체결가를 준다.
     * 종목 상세(StockService)가 이때만 예상 체결가(expCntrPric)를 응답에 넣는다.
     * (실제 거래소의 개장 전 동시호가는 08:30 부터지만 화면 표시는 예전부터 개장 10분 전부터)
     */
    fun isCallAuction(t: LocalTime): Boolean =
        (!t.isBefore(openCallStart) && t.isBefore(open)) || (!t.isBefore(closeCallStart) && t.isBefore(close))

    /** 보유 종목 조회를 NXT 기준으로 해야 하는지 (마감 10분 후부터, 평소 15:40~). 보유 목록(HoldingService)의 거래소 구분에 씀 */
    fun isKrxHoldingClose(t: LocalTime): Boolean = !t.isBefore(holdingClose)

    /** 국내 종목 가격 알림을 돌릴 시간인지 (NXT 시작 1분 후 ~ 20:00, 평소 08:01~20:00). PriceAlertScheduler 에서 씀 */
    fun isStockAlertTime(t: LocalTime): Boolean = !t.isBefore(nxtOpen.plus(STOCK_ALERT_DELAY)) && t.isBefore(nxtClose)
}

/**
 * 평소 시각에 맞춰 둔 하루 한 번 작업이 개장·마감 중 어느 쪽을 따라따라 밀리는지 (KrSessionTaskScheduler.registerDaily).
 * OPEN: 개장 기준 (모의투자 1차 08:50·2차 09:01) / CLOSE: 마감 기준 (국내 마감 브리핑 16:05)
 */
enum class KrSessionAnchor { OPEN, CLOSE }
