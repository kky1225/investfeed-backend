package com.example.investfeed.kiwoom.time.service

import com.example.investfeed.kiwoom.config.MarketType
import com.example.investfeed.kiwoom.time.dto.TimeNowRes
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Service
class TimeService() {
    fun timeNow(): TimeNowRes {
        val now = System.currentTimeMillis()
        var nowEpoch = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Seoul"))
        val time = nowEpoch.toLocalTime()
        var marketType: String? = null
        var isMarketOpen = false

        if (time.isAfter(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(15, 30))) {
            marketType = MarketType.KRX.name
            isMarketOpen = true
        } else if (time.isAfter(LocalTime.of(8, 0)) && time.isBefore(LocalTime.of(20, 0))) {
            marketType = MarketType.NXT.name
            isMarketOpen = true
        } else if(time.isAfter(LocalTime.of(20, 30))) {
            nowEpoch = nowEpoch.plusDays(1)
        }

        val startMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), LocalTime.of(9, 0))
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        val endMarketTime = LocalDateTime.of(nowEpoch.toLocalDate(), LocalTime.of(20, 0))
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()

        return TimeNowRes(
            time = now,
            marketType = marketType,
            startMarketTime = startMarketTime,
            endMarketTime = endMarketTime,
            isMarketOpen = isMarketOpen,
        )
    }
}