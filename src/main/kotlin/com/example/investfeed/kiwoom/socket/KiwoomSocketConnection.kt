package com.example.investfeed.kiwoom.socket

import com.example.investfeed.kiwoom.config.KiwoomWebSocketClient
import com.example.investfeed.kiwoom.socket.dto.KiwoomSocketOwner
import com.example.investfeed.kiwoom.socket.dto.KiwoomStream
import com.example.investfeed.kiwoom.socket.dto.KiwoomStreamReq
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 키움 키 하나의 소켓 연결과 등록 상태. 키 단위로 분리돼 다른 계정의 화면 등록·연결 지연에 영향받지 않는다.
 * 상시 구독은 서버 감시용이라 스케줄러 계정 키의 연결에만 올라간다.
 */
class KiwoomSocketConnection(
    private val objectMapper: ObjectMapper,
    private val deliver: (loginIds: Set<String>, message: String) -> Unit,
    private val onPersistentTick: (entry: JsonNode) -> Unit,
    private val onDisconnected: (loginIds: Set<String>) -> Unit,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val CONNECT_TIMEOUT_SEC = 5L
        private const val LOGIN_TIMEOUT_SEC = 5L
    }

    private val lock = Any()

    @Volatile
    private var client: KiwoomWebSocketClient? = null

    // 이 연결로 화면을 등록한 계정. 키움 시세는 이 계정들의 세션에만 전달된다
    private val subscribers = ConcurrentHashMap.newKeySet<String>()

    // 화면이 등록한 항목 (그룹별). 상시 항목은 포함하지 않는다
    private val registered = ConcurrentHashMap<String, List<KiwoomStream>>()

    // 서버가 화면과 무관하게 항상 받는 항목. 전용 그룹(SYSTEM_GRP_NO)에 한 번 등록하고 화면 등록과 섞지 않는다
    @Volatile
    private var persistent: KiwoomStream? = null

    @Volatile
    private var persistentSent = false

    private var idleSince: LocalDateTime? = null

    fun register(owner: KiwoomSocketOwner, groups: Map<String, List<KiwoomStream>>) {
        synchronized(lock) {
            val connected = connection(owner.accessToken) ?: return

            subscribers += owner.loginId
            idleSince = null

            // 화면 등록이 새 연결을 열었으면 상시 항목도 바로 다시 등록 (다음 매분 점검까지 공백 방지)
            sendPersistentIfNeeded(connected)

            groups.forEach { (grpNo, data) ->
                when {
                    data.isNotEmpty() -> {
                        connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(grp_no = grpNo, data = data)))
                        registered[grpNo] = data
                    }

                    else -> removeGroup(connected, grpNo)
                }
            }
        }
    }

    /** 화면이 등록할 항목이 없을 때 이전 화면 등록을 해지한다. 연결과 상시 항목은 유지한다 */
    fun clearScreen() {
        synchronized(lock) {
            val current = client?.takeIf { it.isOpen && it.loginSucceeded } ?: return
            registered.keys.toList().forEach { removeGroup(current, it) }
        }
    }

    fun ensurePersistent(owner: KiwoomSocketOwner, stream: KiwoomStream) {
        synchronized(lock) {
            persistent = stream
            val connected = connection(owner.accessToken) ?: return
            sendPersistentIfNeeded(connected)
        }
    }

    fun isPersistentConnected(): Boolean = client?.let { it.isOpen && it.loginSucceeded } == true && persistentSent

    fun removeSubscriber(loginId: String) {
        subscribers -= loginId
    }

    /**
     * 화면 이동마다 브라우저 세션이 닫혔다 열리므로 [idleTimeout] 동안 세션이 없을 때만 정리한다.
     * 상시 구독이 있는 연결은 유지하고 화면 등록만 해지한다.
     */
    fun releaseIfIdle(now: LocalDateTime, idleTimeout: Duration, hasSession: (Set<String>) -> Boolean) {
        synchronized(lock) {
            val current = client ?: return

            if (hasSession(subscribers.toSet())) {
                idleSince = null
                return
            }

            val since = idleSince ?: now.also { idleSince = it }
            if (Duration.between(since, now) < idleTimeout) return

            idleSince = null
            when {
                persistent == null -> close()
                current.isOpen && current.loginSucceeded -> {
                    registered.keys.toList().forEach { removeGroup(current, it) }
                    subscribers.clear()
                }
            }
        }
    }

    fun close() {
        synchronized(lock) {
            client?.close()
            client = null
            registered.clear()
            subscribers.clear()
            persistent = null
            persistentSent = false
            idleSince = null
        }
    }

    private fun removeGroup(connected: KiwoomWebSocketClient, grpNo: String) {
        registered.remove(grpNo)?.let { prev ->
            connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(trnm = "REMOVE", grp_no = grpNo, data = prev)))
        }
    }

    private fun sendPersistentIfNeeded(connected: KiwoomWebSocketClient) {
        val stream = persistent ?: return
        if (persistentSent) return
        connected.send(objectMapper.writeValueAsString(KiwoomStreamReq(grp_no = KiwoomSocketManager.SYSTEM_GRP_NO, data = listOf(stream))))
        persistentSent = true
    }

    private fun connection(accessToken: String): KiwoomWebSocketClient? {
        client?.takeIf { it.isOpen && it.loginSucceeded }?.let { return it }

        client?.close()
        registered.clear()
        persistentSent = false

        val fresh = KiwoomWebSocketClient(accessToken, ::onRealTime, ::handleClosed)

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

    /**
     * 키움 소켓이 닫히면 불린다. 우리가 닫거나 교체한 연결이면 client 가 이미 달라 무시한다.
     * 키움 쪽에서 끊은 경우에만 상태를 비우고, 보던 계정의 화면에 재연결을 요청한다.
     */
    private fun handleClosed(closed: KiwoomWebSocketClient) {
        val viewers = synchronized(lock) {
            if (client !== closed) return
            client = null
            registered.clear()
            persistentSent = false
            idleSince = null
            subscribers.toSet().also { subscribers.clear() }
        }
        if (viewers.isNotEmpty()) onDisconnected(viewers)
    }

    private fun onRealTime(message: String) {
        val viewers = subscribers.toSet()
        val persistentKeys = persistent?.let { s -> s.item.orEmpty().flatMap { item -> s.type.map { it to item.toString() } } }.orEmpty().toSet()
        if (persistentKeys.isEmpty()) return deliver(viewers, message)

        val root = runCatching { objectMapper.readTree(message) }.getOrNull() ?: return deliver(viewers, message)
        val entries = root.get("data")?.toList().orEmpty()
        val screenKeys = registered.values.flatten().flatMap { s -> s.item.orEmpty().flatMap { item -> s.type.map { it to item.toString() } } }.toSet()

        entries.filter { key(it) in persistentKeys }.forEach { onPersistentTick(it) }

        val forScreen = entries.filter { key(it) !in persistentKeys || key(it) in screenKeys }
        when (forScreen.size) {
            entries.size -> deliver(viewers, message)
            0 -> Unit
            else -> deliver(viewers, objectMapper.writeValueAsString((root as ObjectNode).set<JsonNode>("data", objectMapper.valueToTree(forScreen))))
        }
    }

    private fun key(entry: JsonNode): Pair<String, String> = entry.get("type")?.asText().orEmpty() to entry.get("item")?.asText().orEmpty()
}
