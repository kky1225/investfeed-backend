package com.example.investfeed.kiwoom.socket

import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.socket.dto.KiwoomSocketOwner
import com.example.investfeed.kiwoom.socket.dto.KiwoomStream
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** 키움 소켓 연결을 키별로 관리한다. 키움은 키(토큰)당 연결 1개만 허용하므로 계정이 아니라 키가 기준이다 */
@Component
class KiwoomSocketManager(
    private val objectMapper: ObjectMapper,
    private val webSocketHandler: WebSocketHandler,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        // 서버 상시 구독 전용 그룹.
        const val SYSTEM_GRP_NO = "9"

        private val IDLE_TIMEOUT: Duration = Duration.ofMinutes(5)
    }

    private val connections = ConcurrentHashMap<String, KiwoomSocketConnection>()

    // 상시 구독 수신을 받는 쪽. 받는 쪽이 이 매니저를 참조하므로(연결 확인) 생성자 주입 대신 등록으로 연결해 순환을 피한다
    private val handlers = CopyOnWriteArrayList<PersistentStreamHandler>()

    fun addHandler(handler: PersistentStreamHandler) {
        handlers += handler
    }

    fun register(owner: KiwoomSocketOwner, groups: Map<String, List<KiwoomStream>>) {
        connectionOf(owner.keyId).register(owner, groups)
    }

    fun ensurePersistent(owner: KiwoomSocketOwner, stream: KiwoomStream) {
        connectionOf(owner.keyId).ensurePersistent(owner, stream)
    }

    fun isConnected(): Boolean = connections.values.any { it.isPersistentConnected() }

    /** API 키 삭제 시 해당 키의 연결을 끊는다 */
    fun disconnect(keyId: String) {
        connections[keyId]?.close()
    }

    /** 같은 키를 쓰는 다른 계정이 남아 있으면 연결은 두고 이 계정으로 시세를 보내지 않게만 한다 */
    fun removeSubscriber(keyId: String, loginId: String) {
        connections[keyId]?.removeSubscriber(loginId)
    }

    /** 한 연결의 실패가 다른 연결 정리를 막지 않도록 끝까지 돌고, 실패는 모니터링에 남도록 다시 던진다 */
    fun releaseIdleConnections() {
        val now = LocalDateTime.now()
        connections.values
            .mapNotNull { connection ->
                runCatching { connection.releaseIfIdle(now, IDLE_TIMEOUT, webSocketHandler::hasSession) }
                    .onFailure { log.error(it) { "유휴 키움 소켓 정리 실패" } }
                    .exceptionOrNull()
            }
            .firstOrNull()
            ?.let { throw it }
    }

    private fun connectionOf(keyId: String): KiwoomSocketConnection = connections.computeIfAbsent(keyId) {
        KiwoomSocketConnection(objectMapper, webSocketHandler::sendTo, ::dispatchPersistentTick)
    }

    private fun dispatchPersistentTick(entry: JsonNode) {
        handlers.forEach { h ->
            runCatching { h.onPersistentTick(entry.get("type").asText(), entry.get("item").asText(), entry.get("values")) }
                .onFailure { log.error(it) { "상시 구독 수신 처리 실패: ${h::class.simpleName} ${entry.get("type")?.asText()}/${entry.get("item")?.asText()}" } }
        }
    }

    @PreDestroy
    fun shutdown() {
        connections.values.forEach { it.close() }
    }
}
