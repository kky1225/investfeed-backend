package com.example.investfeed.kiwoom.realtime.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.realtime.dto.KiwoomGoldPriceStreamReq
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class RealTimeClient(
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun sectIndexListStream(
        req: SectIndexListStreamReq
    ) {
        log.info { "sectIndexListStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        if(isMarketOpen()) {
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

        if(isMarketOpen()) {
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

    private fun isMarketOpen(): Boolean {
        val now = LocalTime.now()

        return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(15, 30))
    }
}