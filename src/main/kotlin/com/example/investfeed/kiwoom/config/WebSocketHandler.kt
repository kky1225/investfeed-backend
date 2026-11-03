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

    fun closeSessions(loginId: String) {
        sessions.filter { it.loginId() == loginId }.forEach { session ->
            runCatching { session.close(CloseStatus.NORMAL) }
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
                log.error { "실시간 전송 실패 : sessionId=${session.id}, ${e.message}" }
                sessions.remove(session)
                runCatching { session.close(CloseStatus.SESSION_NOT_RELIABLE) }
            }
        }
    }

    private fun WebSocketSession.loginId(): String? = attributes[StreamHandshakeInterceptor.LOGIN_ID] as? String
}
