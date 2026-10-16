package com.example.investfeed.kiwoom.socket

import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.socket.dto.KiwoomStream
import com.example.investfeed.kiwoom.socket.dto.KiwoomStreamReq
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
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

        // 서버 상시 구독 전용 그룹.
        const val SYSTEM_GRP_NO = "9"
    }

    private val lock = Any()
    private var client: KiwoomWebSocketClient? = null

    // 화면이 등록한 항목 (그룹별). 상시 항목은 포함하지 않는다
    private val registered = ConcurrentHashMap<String, List<KiwoomStream>>()

    // 서버가 화면과 무관하게 항상 받는 항목. 전용 그룹(SYSTEM_GRP_NO)에 한 번 등록하고 화면 등록과 섞지 않는다
    @Volatile
    private var persistent: KiwoomStream? = null

    @Volatile
    private var persistentSent = false

    // 상시 구독 수신을 받는 쪽. 받는 쪽이 이 매니저를 참조하므로(연결 확인) 생성자 주입 대신 등록으로 연결해 순환을 피한다
    private val handlers = CopyOnWriteArrayList<PersistentStreamHandler>()

    fun addHandler(handler: PersistentStreamHandler) {
        handlers += handler
    }

    fun register(accessToken: String, groups: Map<String, List<KiwoomStream>>) {
        synchronized(lock) {
            val connected = connection(accessToken) ?: return

            // 화면 등록이 새 연결을 열었으면 상시 항목도 바로 다시 등록 (다음 매분 점검까지 공백 방지)
            sendPersistentIfNeeded(connected)

            groups.forEach { (grpNo, data) ->
                when {
                    data.isNotEmpty() -> {
                        connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(grp_no = grpNo, data = data)))
                        registered[grpNo] = data
                    }

                    else -> registered.remove(grpNo)?.let { prev ->
                        connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(trnm = "REMOVE", grp_no = grpNo, data = prev)))
                    }
                }
            }
        }
    }

    fun ensurePersistent(accessToken: String, stream: KiwoomStream) {
        synchronized(lock) {
            persistent = stream
            val connected = connection(accessToken) ?: return
            sendPersistentIfNeeded(connected)
        }
    }

    fun isConnected(): Boolean = client?.let { it.isOpen && it.loginSucceeded } == true && persistentSent

    private fun sendPersistentIfNeeded(connected: KiwoomWebSocketClient) {
        val stream = persistent ?: return
        if (persistentSent) return
        connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(grp_no = SYSTEM_GRP_NO, data = listOf(stream))))
        persistentSent = true
    }

    private fun connection(accessToken: String): KiwoomWebSocketClient? {
        client?.takeIf { it.isOpen && it.loginSucceeded }?.let { return it }

        client?.close()
        registered.clear()
        persistentSent = false

        val fresh = KiwoomWebSocketClient(accessToken) { onRealTime(it) }

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

    /** 상시 항목 수신분은 서버 내부로만 전달하고, 화면이 등록하지 않은 항목은 브라우저로 보내지 않는다 */
    private fun onRealTime(message: String) {
        val persistentKeys = persistent?.let { s -> s.item.orEmpty().flatMap { item -> s.type.map { it to item.toString() } } }.orEmpty().toSet()
        if (persistentKeys.isEmpty()) return webSocketHandler.broadcast(message)

        val root = runCatching { objectMapper.readTree(message) }.getOrNull() ?: return webSocketHandler.broadcast(message)
        val entries = root.get("data")?.toList().orEmpty()
        val screenKeys = registered.values.flatten().flatMap { s -> s.item.orEmpty().flatMap { item -> s.type.map { it to item.toString() } } }.toSet()

        entries.filter { key(it) in persistentKeys }.forEach { entry ->
            handlers.forEach { h ->
                runCatching { h.onPersistentTick(entry.get("type").asText(), entry.get("item").asText(), entry.get("values")) }
                    .onFailure { log.error(it) { "상시 구독 수신 처리 실패: ${h::class.simpleName} ${key(entry)}" } }
            }
        }

        val forScreen = entries.filter { key(it) !in persistentKeys || key(it) in screenKeys }
        when (forScreen.size) {
            entries.size -> webSocketHandler.broadcast(message)
            0 -> Unit
            else -> webSocketHandler.broadcast(objectMapper.writeValueAsString((root as ObjectNode).set<JsonNode>("data", objectMapper.valueToTree(forScreen))))
        }
    }

    private fun key(entry: JsonNode): Pair<String, String> = entry.get("type")?.asText().orEmpty() to entry.get("item")?.asText().orEmpty()

    @PreDestroy
    fun shutdown() {
        synchronized(lock) {
            client?.close()
            client = null
        }
    }
}
