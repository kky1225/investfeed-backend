package com.example.investfeed.kiwoom.sect.service

import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class SectSocketService(
    private val kiwoomWebSocketClient: KiwoomWebSocketClient,
    private val objectMapper: ObjectMapper,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    fun sectIndexListStream(
        accessToken: String,
        req: SectIndexListStreamReq
    ) {
        log.info { "sectIndexListStream $req" }

        kiwoomWebSocketClient.setAccessToken(accessToken)
        kiwoomWebSocketClient.connectBlocking()

        kiwoomWebSocketClient.sendRealTimeHandler(
            trnm = "REAL",
            handler = {
                log.info { "실시간 데이터 :  $it" }
                //webSocketHandler.broadcast(it)
            }
        )

        kiwoomWebSocketClient.sendRequest(
            request = objectMapper.writeValueAsString(req),
            trnm = req.trnm,
            handler = {
                log.info { "1111 :  $it" }
            }
        )
    }
}