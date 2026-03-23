package com.example.investfeed.kiwoom.time.service

import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.config.ExchangeType
import com.example.investfeed.kiwoom.config.MarketType
import com.example.investfeed.kiwoom.time.dto.req.TimeNowReq
import com.example.investfeed.kiwoom.time.dto.res.TimeNowRes
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class TimeService(
    private val holidayService: HolidayService,
) {
    fun timeNow(
        req: TimeNowReq
    ): TimeNowRes {
        val now = System.currentTimeMillis()
        var nowEpoch = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"))
        val nowTime = nowEpoch.toLocalTime()
        var exchangeType: String? = null
        var isMarketOpen = false
        var startMarketTime: Long
        var endMarketTime: Long

        val krxOpen  = LocalTime.of(9, 0)
        val krxClose = LocalTime.of(15, 30)
        val nxtOpen  = LocalTime.of(8, 0)
        val nxtClose = LocalTime.of(20, 0)

        val isHoliday = holidayService.isHoliday()

        if (req.marketType in listOf(MarketType.INDEX, MarketType.COMMODITY)) {
            if(!isHoliday && !nowTime.isBefore(krxOpen) && nowTime.isBefore(krxClose)) {
                exchangeType = ExchangeType.KRX.name
                isMarketOpen = true
            }

            if (nowTime.isAfter(LocalTime.of(15, 30))) {
                nowEpoch = nowEpoch.plusDays(1)
            }

            startMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), krxOpen)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()

            endMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), krxClose)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()
        } else {
            if (!isHoliday) {
                if (!nowTime.isBefore(nxtOpen) && nowTime.isBefore(krxOpen)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                } else if (!nowTime.isBefore(krxOpen) && nowTime.isBefore(krxClose)) {
                    exchangeType = ExchangeType.SOR.name
                    isMarketOpen = true
                } else if (!nowTime.isBefore(krxOpen) && nowTime.isBefore(nxtClose)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                }
            }

            if (nowTime.isAfter(LocalTime.of(20, 0))) {
                nowEpoch = nowEpoch.plusDays(1)
            }

            startMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), nxtOpen)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()

            endMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), nxtClose)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()
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
}