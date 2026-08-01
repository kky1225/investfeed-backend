package com.example.investfeed.kiwoom.us.stock.client

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

        // 미국은 주간거래(대체거래소)+프리+정규+애프터로 한국시간 08~09시경 외 상시 체결이 존재
        // → 시간 게이트 없이 항상 등록 (체결 없는 시간엔 메시지가 오지 않을 뿐)
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
