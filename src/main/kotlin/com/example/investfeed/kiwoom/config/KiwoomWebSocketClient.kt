package com.example.investfeed.kiwoom.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KiwoomWebSocketClient(
    private val accessToken: String,
    private val onRealTime: (String) -> Unit,
) : WebSocketClient(URI(URL)) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val URL = "wss://api.kiwoom.com:10000/api/dostk/websocket"
    }

    private val loginLatch = CountDownLatch(1)

    @Volatile
    var loginSucceeded = false
        private set

    override fun onOpen(handshake: ServerHandshake?) {
        send(jacksonObjectMapper().writeValueAsString(LoginStreamReq(token = accessToken)))
    }

    override fun onMessage(message: String?) {
        if (message == null) return

        try {
            val rootNode = jacksonObjectMapper().readTree(message)

            when (rootNode.get("trnm")?.asText()) {
                "PING" -> send(message)

                "LOGIN" -> {
                    loginSucceeded = rootNode.get("return_code")?.asInt() == 0
                    if (!loginSucceeded) {
                        log.error { "소켓 로그인 실패 : ${rootNode.get("return_msg")?.asText()}" }
                    }
                    loginLatch.countDown()
                }

                "REAL" -> onRealTime(message)

                else -> {
                    if ((rootNode.get("return_code")?.asInt() ?: 0) != 0) {
                        log.error { "소켓 응답 오류 : ${rootNode.get("return_msg")?.asText()}" }
                    }
                }
            }
        } catch (e: Exception) {
            log.error { "소켓 메시지 처리 실패 : ${e.message}" }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        // 키움은 같은 계정으로 새 연결이 열리면 기존 연결을 "Bye"로 끊는다. 원인 추적에 필요해 WARN 으로 남긴다.
        log.warn { "소켓 종료 : code=$code, reason=$reason, remote=$remote" }
        loginLatch.countDown()
    }

    override fun onError(e: Exception?) {
        log.error { "소켓 오류 : $e" }
    }

    /** 로그인 응답까지 대기. 성공해야 등록 전문을 보낼 수 있다. */
    fun awaitLogin(timeout: Long, unit: TimeUnit): Boolean =
        loginLatch.await(timeout, unit) && loginSucceeded
}
