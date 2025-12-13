package com.example.investfeed.kiwoom.stock.client

import com.example.investfeed.kiwoom.annotation.KiwoomToken
import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.exception.AccessTokenNotFoundException
import com.example.investfeed.kiwoom.stock.entity.req.KiwoomStockStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class StockSocketClient(
    private val objectMapper: ObjectMapper,
    private val redisTemplate: RedisTemplate<String, String>,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    @KiwoomToken
    fun stockListStream(
        req: KiwoomStockStreamReq
    ) {
        log.debug { "stockListStream $req" }

        val accessToken = redisTemplate.opsForValue().get("kiwoom:access_token")
        accessToken ?: throw AccessTokenNotFoundException()

        val nxtOpen  = LocalTime.of(8, 0)
        val nxtClose = LocalTime.of(20, 0)

        val now = LocalTime.now()

        if(isMarketOpen(now, nxtOpen, nxtClose)) {
//            if (!now.isBefore(nxtOpen) && now.isBefore(krxOpen)) {
//                req.data!!.stream().map { i ->
//                    i.item?.let { it + ExchangeType.NXT }
//                }
//            } else if (!now.isBefore(krxOpen) && now.isBefore(krxClose)) {
//                req.data!!.stream().map { i ->
//                    i.item?.let { it + ExchangeType.SOR }
//                }
//            } else if (!now.isBefore(krxOpen) && now.isBefore(nxtClose)) {
//                req.data!!.stream().map { i ->
//                    i.item?.let { it + ExchangeType.NXT }
//                }
//            }
//
//            val kiwoomWebSocketClient = KiwoomWebSocketClient()
//            kiwoomWebSocketClient.setAccessToken(accessToken)
//            kiwoomWebSocketClient.connectBlocking()
//
//            kiwoomWebSocketClient.sendRealTimeHandler(
//                trnm = "REAL",
//                handler = {
//                    webSocketHandler.broadcast(it)
//                }
//            )
//
//            kiwoomWebSocketClient.sendRequest(
//                request = objectMapper.writeValueAsString(req),
//                trnm = req.trnm,
//                handler = {
//
//                }
//            )

            val kiwoomWebSocketClient = KiwoomWebSocketClient()
            kiwoomWebSocketClient.setAccessToken(accessToken)
            kiwoomWebSocketClient.connectBlocking()

            kiwoomWebSocketClient.sendRealTimeHandler(
                trnm = "REAL",
                handler = {
                    webSocketHandler.broadcast(it)
                }
            )

            kiwoomWebSocketClient.sendRequest(
                request = objectMapper.writeValueAsString(req),
                trnm = req.trnm,
                handler = {

                }
            )
        }
    }

    private fun isMarketOpen(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return !now.isBefore(start) && !now.isAfter(end)
    }
}