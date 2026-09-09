package com.example.investfeed.kiwoom.socket

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomStream
import com.example.investfeed.kiwoom.socket.dto.KiwoomStreamReq
import com.example.investfeed.kiwoom.socket.dto.StreamEntry
import com.example.investfeed.kiwoom.socket.dto.StreamMarket
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class KiwoomStreamClient(
    private val socketManager: KiwoomSocketManager,
    private val authClient: AuthClient,
    private val holidayService: HolidayService,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun register(vararg entries: StreamEntry) {
        val data = entries
            .filter { it.items.isNotEmpty() && isOpen(it.market) }
            .map { KiwoomStream(item = it.items, type = it.types) }

        if (data.isEmpty()) return

        log.debug { "실시간 등록 $data" }

        socketManager.send(
            accessToken = authClient.getCurrentAccessToken(),
            req = KiwoomStreamReq(data = data)
        )
    }

    private fun isOpen(market: StreamMarket): Boolean = when (market) {
        StreamMarket.US -> true
        StreamMarket.KRX -> !holidayService.isHoliday() && MarketTimeUtil.isKrxOpen()
        StreamMarket.NXT -> !holidayService.isHoliday() && MarketTimeUtil.isNxtOpen()
    }
}
