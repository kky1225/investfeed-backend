package com.example.investfeed.domain.time.service

import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.global.holiday.KrMarketSessionService
import com.example.investfeed.domain.time.ExchangeType
import com.example.investfeed.domain.time.MarketType
import com.example.investfeed.domain.time.dto.req.TimeNowReq
import com.example.investfeed.domain.time.dto.res.TimeNowRes
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class TimeService(
    private val holidayService: HolidayService,
    private val krMarketSessionService: KrMarketSessionService,
) {
    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }

    fun getCurrentTime(
        req: TimeNowReq
    ): TimeNowRes {
        val now = System.currentTimeMillis()
        val nowEpoch = Instant.ofEpochMilli(now).atZone(KST)
        val today = nowEpoch.toLocalDate()
        val nowTime = nowEpoch.toLocalTime()
        val session = krMarketSessionService.of(today)
        var exchangeType: String? = null
        var isMarketOpen = false
        val startMarketTime: Long
        val endMarketTime: Long

        val isHoliday = holidayService.isHoliday()

        if (req.marketType in listOf(MarketType.INDEX, MarketType.COMMODITY)) {
            if (!isHoliday && session.isKrxOpen(nowTime)) {
                exchangeType = ExchangeType.KRX.name
                isMarketOpen = true
            }

            // 마감 후면 다음 날 장 시각 (다음 날이 수능일이면 그날 시각)
            val day = if (nowTime.isAfter(session.close)) krMarketSessionService.of(today.plusDays(1)) else session
            startMarketTime = epochMilli(day.date, day.open)
            endMarketTime = epochMilli(day.date, day.close)
        } else {
            if (!isHoliday) {
                if (!nowTime.isBefore(session.nxtOpen) && nowTime.isBefore(session.open)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                } else if (session.isKrxOpen(nowTime)) {
                    exchangeType = ExchangeType.SOR.name
                    isMarketOpen = true
                } else if (!nowTime.isBefore(session.open) && nowTime.isBefore(session.nxtClose)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                }
            }

            val day = if (nowTime.isAfter(session.nxtClose)) krMarketSessionService.of(today.plusDays(1)) else session
            startMarketTime = epochMilli(day.date, day.nxtOpen)
            endMarketTime = epochMilli(day.date, day.nxtClose)
        }

        return TimeNowRes(
            time = now,
            marketType = req.marketType.name,
            isMarketOpen = isMarketOpen,
            exchangeType = exchangeType,
            startMarketTime = startMarketTime,
            endMarketTime = endMarketTime
        )
    }

    private fun epochMilli(date: LocalDate, time: LocalTime): Long =
        LocalDateTime.of(date, time).atZone(KST).toInstant().toEpochMilli()
}
