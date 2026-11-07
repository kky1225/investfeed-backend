package com.example.investfeed.toss.socket

import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.toss.auth.service.TossAuthClient
import com.example.investfeed.toss.config.TossWebSocketClient
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 토스 웹소켓 연결을 회원별로 관리한다. 토스 토큰은 회원 본인 키로만 발급되므로 키움과 달리 회원이 기준이다.
 * 키움과 같은 규칙: 화면 이동에도 연결은 유지하고 다음 화면 등록이 구독을 덮어쓰며, 화면 소켓이 [IDLE_TIMEOUT] 동안 없으면 끊는다.
 * 화면 등록(요청 스레드)과 정리(스케줄러 스레드)가 같은 회원을 동시에 건드릴 수 있어 회원별로 잠근다.
 */
@Component
class TossSocketManager(
    private val objectMapper: ObjectMapper,
    private val tossAuthClient: TossAuthClient,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val CONNECT_TIMEOUT_SEC = 5L
        private val IDLE_TIMEOUT: Duration = Duration.ofMinutes(5)   // 키움 유휴 정리와 같은 값
    }

    private class Connection(val client: TossWebSocketClient) {
        @Volatile
        var symbols: Set<String> = emptySet()

        @Volatile
        var idleSince: Instant? = null
    }

    private val connections = ConcurrentHashMap<String, Connection>()
    private val locks = ConcurrentHashMap<String, Any>()

    /**
     * 구독을 [symbols] 로 맞춘다. 연결이 없거나 끊겼으면 새로 연결한다.
     * [onLost]: 이 연결이 토스 쪽에서 끊겼을 때 (토큰 재발급·서버 배포 등). 일부러 끊은 경우는 부르지 않는다
     */
    fun subscribe(
        loginId: String,
        symbols: Set<String>,
        onTrade: (loginId: String, symbol: String, price: String) -> Unit,
        onLost: (loginId: String) -> Unit,
    ) = synchronized(lock(loginId)) {
        val connection = connections[loginId]?.takeIf { it.client.isOpen }
            ?: connect(loginId, onTrade, onLost)
            ?: return
        connection.idleSince = null
        if (connection.symbols != symbols) {
            connection.client.declare(symbols)
            connection.symbols = symbols
        }
    }

    /** 화면 구독만 비운다. 연결은 유지하고, 연결이 없으면 새로 만들지 않는다 */
    fun clear(loginId: String) = synchronized(lock(loginId)) {
        val connection = connections[loginId]?.takeIf { it.client.isOpen && it.symbols.isNotEmpty() } ?: return
        connection.client.declare(emptyList())
        connection.symbols = emptySet()
    }

    /** 화면 이동마다 브라우저 세션이 닫혔다 열리므로 [IDLE_TIMEOUT] 동안 그 회원의 화면 소켓이 없을 때만 끊는다. 끊은 회원을 돌려준다 */
    fun releaseIdleConnections(now: Instant = Instant.now()): Set<String> =
        connections.entries.filter { (loginId, connection) ->
            if (webSocketHandler.hasSession(setOf(loginId))) {
                connection.idleSince = null
                return@filter false
            }
            val since = connection.idleSince ?: now.also { connection.idleSince = it }
            Duration.between(since, now) >= IDLE_TIMEOUT
        }.map { it.key }.onEach(::disconnect).toSet()

    private fun disconnect(loginId: String) = synchronized(lock(loginId)) {
        connections.remove(loginId)?.client?.close()
    }

    private fun connect(loginId: String, onTrade: (String, String, String) -> Unit, onLost: (String) -> Unit): Connection? {
        connections.remove(loginId)?.client?.close()

        tossAuthClient.ensureAccessToken(loginId)
        var connection: Connection? = null
        val client = TossWebSocketClient(
            accessToken = tossAuthClient.getAccessToken(loginId),
            objectMapper = objectMapper,
            onTrade = { symbol, price -> onTrade(loginId, symbol, price) },
            // 지금 쓰는 연결이 닫혔을 때만 알린다 (disconnect 는 먼저 목록에서 빼므로 해당 없음)
            onClosed = { if (connection?.let { connections.remove(loginId, it) } == true) onLost(loginId) },
        )

        if (!client.connectBlocking(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            log.error { "토스 실시간 연결 실패 : loginId=$loginId" }
            client.close()
            return null
        }

        return Connection(client).also {
            connection = it
            connections[loginId] = it
        }
    }

    private fun lock(loginId: String): Any = locks.computeIfAbsent(loginId) { Any() }

    @PreDestroy
    fun shutdown() {
        connections.values.forEach { it.client.close() }
        connections.clear()
    }
}
