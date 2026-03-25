package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class StockSocketClient(
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun stockListStream(
        req: KiwoomStockStreamReq
    ) {
        log.debug { "stockListStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        val nxtOpen  = LocalTime.of(8, 0)
        val nxtClose = LocalTime.of(20, 0)

        val now = LocalTime.now()

        if(isMarketOpen(now, nxtOpen, nxtClose)) {
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

    private fun isMarketOpen(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return !now.isBefore(start) && !now.isAfter(end)
    }
}