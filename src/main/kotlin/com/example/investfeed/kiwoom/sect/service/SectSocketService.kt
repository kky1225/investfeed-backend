package com.example.investfeed.kiwoom.sect.service

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class SectSocketService(
    private val objectMapper: ObjectMapper,
    private val redisTemplate: RedisTemplate<String, String>,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun sectIndexListStream(
        req: SectIndexListStreamReq
    ) {
        log.info { "sectIndexListStream $req" }

        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        val kiwoomWebSocketClient = KiwoomWebSocketClient()
        kiwoomWebSocketClient.setAccessToken(accessToken)
        kiwoomWebSocketClient.connectBlocking()

        kiwoomWebSocketClient.sendRealTimeHandler(
            trnm = "REAL",
            handler = {
                log.info { "실시간 데이터 :  $it" }
                webSocketHandler.broadcast(it)
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