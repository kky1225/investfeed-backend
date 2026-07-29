package com.example.investfeed.kiwoom.us.stock.client

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomUsWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.us.stock.dto.req.KiwoomUsStockStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class UsStockSocketClient(
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun usStockListStream(
        req: KiwoomUsStockStreamReq
    ) {
        log.debug { "usStockListStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        if(MarketTimeUtil.isUsOpen()) {
            val kiwoomUsWebSocketClient = KiwoomUsWebSocketClient()
            kiwoomUsWebSocketClient.setAccessToken(accessToken)
            kiwoomUsWebSocketClient.connectBlocking()

            kiwoomUsWebSocketClient.sendRealTimeHandler(
                trnm = "REAL",
                handler = {
                    webSocketHandler.broadcast(it)
                }
            )

            kiwoomUsWebSocketClient.setRequest(
                request = objectMapper.writeValueAsString(req),
                trnm = req.trnm,
            )
        }
    }
}
