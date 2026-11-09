package com.example.investfeed.upbit.websocket.client

import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UpbitWebSocketClient(
    private val webSocketHandler: WebSocketHandler,
    private val markets: List<String>,
) : WebSocketClient(URI("wss://api.upbit.com/websocket/v1")) {
    private val log = KotlinLogging.logger {}
    private val objectMapper = jacksonObjectMapper()
    private val kstFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss").withZone(ZoneId.of("Asia/Seoul"))

    /** 이 연결에서 메시지 처리 실패를 이미 error 로 남겼는지 (메시지는 단일 읽기 스레드에서 처리됨) */
    @Volatile
    private var messageErrorLogged = false

    override fun onOpen(handshakedata: ServerHandshake?) {
        log.info { "[Upbit WebSocket] 연결 성공" }

        val subscribeMessage = objectMapper.writeValueAsString(
            listOf(
                mapOf("ticket" to "investfeed"),
                mapOf("type" to "ticker", "codes" to markets),
                mapOf("format" to "DEFAULT"),
            )
        )

        send(subscribeMessage)
        log.info { "[Upbit WebSocket] 구독 요청: $subscribeMessage" }
    }

    override fun onMessage(message: String?) {
        // Upbit WebSocket은 보통 바이너리로 응답하므로 이 메서드는 fallback
        if (message == null) return
        processMessage(message)
    }

    override fun onMessage(bytes: ByteBuffer?) {
        if (bytes == null) return
        val message = StandardCharsets.UTF_8.decode(bytes).toString()
        processMessage(message)
    }

    private fun processMessage(message: String) {
        try {
            val rootNode = objectMapper.readTree(message)
            val type = rootNode.get("type")?.asText() ?: return

            if (type == "ticker") {
                val broadcastData = objectMapper.writeValueAsString(
                    mapOf(
                        "type" to "CRYPTO_TICKER",
                        "data" to mapOf(
                            "market" to rootNode.get("code")?.asText(),
                            "tradePrice" to rootNode.get("trade_price")?.asDouble(),
                            "change" to rootNode.get("change")?.asText(),
                            "signedChangeRate" to rootNode.get("signed_change_rate")?.asDouble(),
                            "signedChangePrice" to rootNode.get("signed_change_price")?.asDouble(),
                            "changeRate" to rootNode.get("change_rate")?.asDouble(),
                            "changePrice" to rootNode.get("change_price")?.asDouble(),
                            "accTradePrice24h" to rootNode.get("acc_trade_price_24h")?.asDouble(),
                            "accTradeVolume24h" to rootNode.get("acc_trade_volume_24h")?.asDouble(),
                            "tradeDateTimeKst" to rootNode.get("trade_timestamp")?.asLong()?.let {
                                kstFormatter.format(Instant.ofEpochMilli(it))
                            },
                        )
                    )
                )

                webSocketHandler.broadcast(broadcastData)
            }
        } catch (e: Exception) {
            // 메시지마다 호출되므로 형식이 바뀐 스트림이면 초당 수십 건이 된다. 연결당 첫 건만 error, 이후는 warn
            if (messageErrorLogged) {
                log.warn { "[Upbit WebSocket] 메시지 처리 실패: ${e.javaClass.simpleName}: ${e.message}" }
            } else {
                messageErrorLogged = true
                log.error(e) { "[Upbit WebSocket] 메시지 처리 실패 (이 연결의 이후 실패는 warn): ${message.take(200)}" }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        log.info { "[Upbit WebSocket] 연결 종료: code=$code, reason=$reason, remote=$remote" }
    }

    override fun onError(ex: Exception?) {
        log.error { "[Upbit WebSocket] 오류: ${ex?.message}" }
    }
}
