package com.example.investfeed.kiwoom.config

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.CopyOnWriteArraySet

@Component
class WebSocketHandler: TextWebSocketHandler() {
    private val log = KotlinLogging.logger {}

    companion object {
        // 시세가 없어도 연결이 살아 있음을 화면에 알린다. 프론트 reconnectingSocket.ts 와 값을 맞출 것
        private const val HEARTBEAT = """{"trnm":"PING"}"""

        // 로그아웃·계정 잠금처럼 일부러 끊는 경우. 프론트는 이 코드일 때만 재연결하지 않는다
        private val LOGGED_OUT = CloseStatus(4001, "logged out")
    }

    private val sessions = CopyOnWriteArraySet<WebSocketSession>()

    override fun afterConnectionEstablished(
        session: WebSocketSession
    ) {
        log.info { "afterConnectionEstablished: ${session.id}" }
        sessions.add(session)
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage
    ) {
        log.info { "handleTextMessage: ${message.payload}" }
        session.sendMessage(TextMessage("서버 응답: ${message.payload}"))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info { "afterConnectionClosed: ${session.id}" }
        sessions.remove(session)
    }

    /** 공개 시세(업비트) 전용. 키움 시세는 [sendTo] 로 계정별 전달 */
    fun broadcast(message: String) = send(sessions, message)

    fun sendTo(loginIds: Set<String>, message: String) = send(sessions.filter { it.loginId() in loginIds }, message)

    fun hasSession(loginIds: Set<String>): Boolean = sessions.any { it.isOpen && it.loginId() in loginIds }

    fun sendHeartbeat() = broadcast(HEARTBEAT)

    fun closeSessions(loginId: String) = close(sessions.filter { it.loginId() == loginId }, LOGGED_OUT)

    /** 키움 연결이 끊긴 계정의 세션을 닫아, 화면이 다시 등록하면서 키움 연결도 새로 맺게 한다 */
    fun requestReconnect(loginIds: Set<String>) = close(sessions.filter { it.loginId() in loginIds }, CloseStatus.SERVICE_RESTARTED)

    private fun close(targets: Collection<WebSocketSession>, status: CloseStatus) {
        targets.forEach { session ->
            runCatching { session.close(status) }
                .onFailure { log.error { "실시간 세션 종료 실패 : sessionId=${session.id}, ${it.message}" } }
            sessions.remove(session)
        }
    }

    private fun send(targets: Collection<WebSocketSession>, message: String) {
        val payload = TextMessage(message)

        targets.filter { it.isOpen }.forEach { session ->
            try {
                synchronized(session) { session.sendMessage(payload) }
            } catch (e: Exception) {
                // 브라우저가 닫히거나 네트워크가 끊긴 세션에 보낼 때 나는 일상 오류라 warn. 세션은 정리한다
                log.warn { "실시간 전송 실패 : sessionId=${session.id}, ${e.message}" }
                sessions.remove(session)
                runCatching { session.close(CloseStatus.SESSION_NOT_RELIABLE) }
            }
        }
    }

    private fun WebSocketSession.loginId(): String? = attributes[StreamHandshakeInterceptor.LOGIN_ID] as? String
}
