package com.example.investfeed.domain.calendar.service

import com.example.investfeed.domain.calendar.dto.req.MarketSessionReq
import com.example.investfeed.domain.calendar.dto.res.MarketSessionRes
import com.example.investfeed.domain.calendar.exception.MarketSessionDuplicateException
import com.example.investfeed.domain.calendar.exception.MarketSessionInvalidTimeException
import com.example.investfeed.domain.calendar.exception.MarketSessionNotFoundException
import com.example.investfeed.domain.calendar.exception.MarketSessionTimeRangeException
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.global.holiday.KrMarketSessionService
import com.example.investfeed.global.holiday.KrSessionTaskScheduler
import com.example.investfeed.global.holiday.MarketSessionOverride
import com.example.investfeed.global.holiday.MarketSessionOverrideRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 국내 장 시각 변경일(수능일 등) 관리. 바꾸면 장 시각 캐시를 비워 서버 재시작 없이 반영한다.
 * 저장이 커밋된 뒤에 캐시를 비워야 다른 요청이 예전 값을 다시 읽어 두지 않는다 → 메서드에 트랜잭션을 두지 않고 save 한 번으로 커밋
 */
@Service
class MarketSessionAdminService(
    private val repository: MarketSessionOverrideRepository,
    private val krMarketSessionService: KrMarketSessionService,
    private val krSessionTaskScheduler: KrSessionTaskScheduler,
) {
    companion object {
        private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }

    fun list(year: Int): List<MarketSessionRes> =
        repository.findAllByMarketAndTradeDateBetweenOrderByTradeDate(
            HolidayService.MARKET_KR, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
        ).map { it.toRes() }

    fun create(req: MarketSessionReq): MarketSessionRes {
        val date = LocalDate.parse(req.date)
        if (repository.existsByMarketAndTradeDate(HolidayService.MARKET_KR, date)) throw MarketSessionDuplicateException()
        val (open, close) = parseTimes(req)
        val saved = repository.save(
            MarketSessionOverride(tradeDate = date, name = req.name.trim(), openTime = open, closeTime = close, preMarket = req.preMarket ?: false)
        )
        applyChange()
        return saved.toRes()
    }

    fun update(id: Long, req: MarketSessionReq): MarketSessionRes {
        val entity = repository.findById(id).orElseThrow { MarketSessionNotFoundException() }
        val date = LocalDate.parse(req.date)
        if (date != entity.tradeDate && repository.existsByMarketAndTradeDate(HolidayService.MARKET_KR, date)) throw MarketSessionDuplicateException()
        val (open, close) = parseTimes(req)
        entity.tradeDate = date
        entity.name = req.name.trim()
        entity.openTime = open
        entity.closeTime = close
        entity.preMarket = req.preMarket ?: false
        entity.updatedAt = LocalDateTime.now()
        val saved = repository.save(entity)
        applyChange()
        return saved.toRes()
    }

    fun delete(id: Long) {
        val entity = repository.findById(id).orElseThrow { MarketSessionNotFoundException() }
        repository.delete(entity)
        applyChange()
    }

    /** 장 시각 캐시를 비우고, 장 시각을 따라 도는 작업(장중 반복·하루 한 번)의 다음 실행 시각을 다시 계산 */
    private fun applyChange() {
        krMarketSessionService.evict()
        krSessionTaskScheduler.reschedule()
    }

    private fun parseTimes(req: MarketSessionReq): Pair<LocalTime, LocalTime> {
        val open = LocalTime.parse(req.openTime, HH_MM)
        val close = LocalTime.parse(req.closeTime, HH_MM)
        if (!close.isAfter(open)) throw MarketSessionInvalidTimeException()
        // 오타 등 잘못된 입력 방지용 안전 범위 (KrMarketSessionService.OPEN_RANGE / CLOSE_RANGE)
        if (open !in KrMarketSessionService.OPEN_RANGE || close !in KrMarketSessionService.CLOSE_RANGE) {
            throw MarketSessionTimeRangeException(
                "개장은 ${KrMarketSessionService.OPEN_RANGE.start.format(HH_MM)}~${KrMarketSessionService.OPEN_RANGE.endInclusive.format(HH_MM)}, " +
                    "마감은 ${KrMarketSessionService.CLOSE_RANGE.start.format(HH_MM)}~${KrMarketSessionService.CLOSE_RANGE.endInclusive.format(HH_MM)} 사이로 입력해주세요."
            )
        }
        return open to close
    }

    private fun MarketSessionOverride.toRes() = MarketSessionRes(
        id = id, date = tradeDate.toString(), name = name,
        openTime = openTime.format(HH_MM), closeTime = closeTime.format(HH_MM), preMarket = preMarket,
    )
}
