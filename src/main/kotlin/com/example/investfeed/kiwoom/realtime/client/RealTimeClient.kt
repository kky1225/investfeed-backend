package com.example.investfeed.kiwoom.realtime.client

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.global.holiday.HolidayService
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.realtime.dto.KiwoomGoldPriceStreamReq
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class RealTimeClient(
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val webSocketHandler: WebSocketHandler,
    private val holidayService: HolidayService,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun sectIndexListStream(
        req: SectIndexListStreamReq
    ) {
        log.info { "sectIndexListStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        if(!holidayService.isHoliday() && MarketTimeUtil.isKrxOpen()) {
            val kiwoomWebSocketClient = KiwoomWebSocketClient()
            kiwoomWebSocketClient.setAccessToken(accessToken)
            kiwoomWebSocketClient.connectBlocking()

            kiwoomWebSocketClient.sendRealTimeHandler(
                trnm = "REAL",
                handler = {
                    webSocketHandler.broadcast(it)
                }
            )

            kiwoomWebSocketClient.setRequest(
                request = objectMapper.writeValueAsString(req),
                trnm = req.trnm,
            )
        }
    }

    @KiwoomToken
    fun goldPriceListStream(
        req: KiwoomGoldPriceStreamReq
    ) {
        log.info { "goldPriceListStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        // 휴장일엔 체결 데이터가 없으므로 소켓 등록 자체를 생략
        if(!holidayService.isHoliday() && MarketTimeUtil.isKrxOpen()) {
            val kiwoomWebSocketClient = KiwoomWebSocketClient()
            kiwoomWebSocketClient.setAccessToken(accessToken)
            kiwoomWebSocketClient.connectBlocking()

            kiwoomWebSocketClient.sendRealTimeHandler(
                trnm = "REAL",
                handler = {
                    webSocketHandler.broadcast(it)
                }
            )

            kiwoomWebSocketClient.setRequest(
                request = objectMapper.writeValueAsString(req),
                trnm = req.trnm,
            )
        }
    }
}
