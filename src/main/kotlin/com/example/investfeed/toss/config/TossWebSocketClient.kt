package com.example.investfeed.toss.config

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

/**
 * 토스 실시간 체결(trade:us) 웹소켓 연결 1개. 계정당 동시 연결 2개·연결당 구독 100건·180초 무송신 시 종료 (공식 연동 가이드).
 * 구독은 선언형(full-replace)이라 [declare] 로 보낸 목록이 곧 현재 구독 전체다.
 */
class TossWebSocketClient(
    accessToken: String,
    private val objectMapper: ObjectMapper,
    private val onTrade: (symbol: String, price: String) -> Unit,
    private val onClosed: (TossWebSocketClient) -> Unit,
) : WebSocketClient(URI(URL), mapOf("Authorization" to "Bearer $accessToken")) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val URL = "wss://openapi-ws.tossinvest.com/ws/v1"
        private const val TRADE_TYPE = "trade:us"
        private const val TOPIC_PREFIX = "$TRADE_TYPE:"
        private const val PING_INTERVAL_SEC = 60   // 공식: 180초 무송신 시 종료, 60초 권장 (표준 ping 프레임도 지원)
    }

    // 라이브러리가 60초마다 표준 ping 을 보내고, 응답이 없으면 연결을 닫는다
    init {
        connectionLostTimeout = PING_INTERVAL_SEC
    }

    /** 연결이 한 번이라도 열렸는지. 열리기 전 오류는 연결 실패라 [TossSocketManager] 가 error 1건을 남긴다 */
    @Volatile
    private var opened = false

    /** 연결 전 마지막 오류. 연결 실패 error 로그에 원인으로 붙인다 */
    @Volatile
    var connectError: Exception? = null
        private set

    override fun onOpen(handshake: ServerHandshake?) {
        opened = true
    }

    fun declare(symbols: Collection<String>) {
        val body = if (symbols.isEmpty()) emptyList() else listOf(mapOf("type" to TRADE_TYPE, "codes" to symbols.toList()))
        send(objectMapper.writeValueAsString(body))
    }

    override fun onMessage(message: String?) {
        if (message == null) return

        try {
            val root = objectMapper.readTree(message)
            when (root.get("type")?.asText()) {
                "message" -> {
                    val symbol = root.get("topic")?.asText()?.takeIf { it.startsWith(TOPIC_PREFIX) }?.removePrefix(TOPIC_PREFIX) ?: return
                    val price = root.get("data")?.get("price")?.asText()?.takeIf { it.isNotBlank() } ?: return
                    onTrade(symbol, price)
                }

                "subscriptions" -> root.get("rejected")?.takeIf { it.size() > 0 }?.let {
                    log.warn { "토스 실시간 구독 일부 거부: $it" }
                }

                "error" -> log.error { "토스 실시간 오류 : ${root.get("error")?.get("code")?.asText()} ${root.get("error")?.get("message")?.asText()}" }
            }
        } catch (e: Exception) {
            log.error { "토스 실시간 수신 처리 실패 : ${e.message}" }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (remote) log.warn { "토스 실시간 연결 종료 : code=$code, reason=$reason" }
        onClosed(this)
    }

    override fun onError(ex: Exception?) {
        if (opened) {
            log.error { "토스 실시간 연결 오류 : ${ex?.message}" }
        } else {
            connectError = ex
            log.warn { "토스 실시간 연결 중 오류 : ${ex?.message}" }
        }
    }
}
