package com.example.investfeed.upbit.websocket.client

import com.example.investfeed.kiwoom.config.WebSocketHandler
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class CryptoStreamClient(
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}
    private var currentClient: UpbitWebSocketClient? = null

    fun cryptoListStream(markets: List<String>) {
        log.info { "cryptoListStream 시작: $markets" }

        currentClient?.let {
            if (it.isOpen) {
                it.close()
            }
        }

        val upbitWebSocketClient = UpbitWebSocketClient(webSocketHandler, markets)
        upbitWebSocketClient.connectBlocking()
        currentClient = upbitWebSocketClient
    }
}
