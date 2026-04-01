package com.example.investfeed.kiwoom.holding.client

import com.example.investfeed.common.util.MarketTimeUtil
import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.auth.service.AuthClient
import com.example.investfeed.kiwoom.holding.dto.req.KiwoomHoldingStreamReq
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class HoldingSocketClient(
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun holdingStream(
        req: KiwoomHoldingStreamReq
    ) {
        log.debug { "holdingStream $req" }

        val accessToken = authClient.getCurrentAccessToken()

        if (MarketTimeUtil.isNxtOpen()) {
            val kiwoomWebSocketClient = KiwoomWebSocketClient()
            kiwoomWebSocketClient.setAccessToken(accessToken)
            kiwoomWebSocketClient.connectBlocking()

            kiwoomWebSocketClient.sendRealTimeHandler(
                trnm = "REAL",
                handler = {
                    webSocketHandler.broadcast(it)
                }
            )

            val streamReq = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = listOf(""),
                        type = listOf("04")
                    ),
                    KiwoomStockStream(
                        item = req.items,
                        type = listOf("0B")
                    )
                )
            )

            kiwoomWebSocketClient.setRequest(
                request = objectMapper.writeValueAsString(streamReq),
                trnm = streamReq.trnm,
            )
        }
    }
}
