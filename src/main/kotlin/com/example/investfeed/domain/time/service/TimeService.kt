package com.example.investfeed.domain.time.service

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.domain.time.ExchangeType
import com.example.investfeed.domain.time.MarketType
import com.example.investfeed.domain.time.dto.req.TimeNowReq
import com.example.investfeed.domain.time.dto.res.TimeNowRes
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class TimeService(
    private val holidayService: HolidayService,
) {
    fun getCurrentTime(
        req: TimeNowReq
    ): TimeNowRes {
        val now = System.currentTimeMillis()
        var nowEpoch = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"))
        val nowTime = nowEpoch.toLocalTime()
        var exchangeType: String? = null
        var isMarketOpen = false
        var startMarketTime: Long
        var endMarketTime: Long

        val isHoliday = holidayService.isHoliday()

        if (req.marketType in listOf(MarketType.INDEX, MarketType.COMMODITY)) {
            if(!isHoliday && MarketTimeUtil.isKrxOpen(nowTime)) {
                exchangeType = ExchangeType.KRX.name
                isMarketOpen = true
            }

            if (nowTime.isAfter(MarketTimeUtil.KRX_CLOSE)) {
                nowEpoch = nowEpoch.plusDays(1)
            }

            startMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), MarketTimeUtil.KRX_OPEN)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()

            endMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), MarketTimeUtil.KRX_CLOSE)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()
        } else {
            if (!isHoliday) {
                if (!nowTime.isBefore(MarketTimeUtil.NXT_OPEN) && nowTime.isBefore(MarketTimeUtil.KRX_OPEN)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                } else if (!nowTime.isBefore(MarketTimeUtil.KRX_OPEN) && nowTime.isBefore(MarketTimeUtil.KRX_CLOSE)) {
                    exchangeType = ExchangeType.SOR.name
                    isMarketOpen = true
                } else if (!nowTime.isBefore(MarketTimeUtil.KRX_OPEN) && nowTime.isBefore(MarketTimeUtil.NXT_CLOSE)) {
                    exchangeType = ExchangeType.NXT.name
                    isMarketOpen = true
                }
            }

            if (nowTime.isAfter(MarketTimeUtil.NXT_CLOSE)) {
                nowEpoch = nowEpoch.plusDays(1)
            }

            startMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), MarketTimeUtil.NXT_OPEN)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli()

            endMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), MarketTimeUtil.NXT_CLOSE)
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
