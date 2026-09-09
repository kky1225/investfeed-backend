package com.example.investfeed.kiwoom.socket

import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.socket.dto.KiwoomStreamReq
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class KiwoomSocketManager(
    private val objectMapper: ObjectMapper,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val CONNECT_TIMEOUT_SEC = 5L
        private const val LOGIN_TIMEOUT_SEC = 5L
    }

    private val lock = Any()
    private var client: KiwoomWebSocketClient? = null

    fun send(accessToken: String, req: KiwoomStreamReq) {
        synchronized(lock) {
            val connected = connection(accessToken) ?: return
            connected.send(objectMapper.writeValueAsString(req))
        }
    }

    private fun connection(accessToken: String): KiwoomWebSocketClient? {
        client?.takeIf { it.isOpen && it.loginSucceeded }?.let { return it }

        client?.close()

        val fresh = KiwoomWebSocketClient(accessToken) { webSocketHandler.broadcast(it) }

        if (!fresh.connectBlocking(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            log.error { "키움 소켓 연결 실패" }
            client = null
            return null
        }

        if (!fresh.awaitLogin(LOGIN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            log.error { "키움 소켓 로그인 실패" }
            fresh.close()
            client = null
            return null
        }

        client = fresh
        return fresh
    }

    @PreDestroy
    fun shutdown() {
        synchronized(lock) {
            client?.close()
            client = null
        }
    }
}
