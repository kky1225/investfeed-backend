package com.example.investfeed.domain.notification.config

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationWebSocketHandler : TextWebSocketHandler() {

    private val log = KotlinLogging.logger {}
    private val userSessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val memberId = memberIdOf(session) ?: run {
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        userSessions.computeIfAbsent(memberId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val memberId = memberIdOf(session) ?: return
        userSessions[memberId]?.remove(session)
        if (userSessions[memberId]?.isEmpty() == true) {
            userSessions.remove(memberId)
        }
        log.info { "알림 WebSocket 종료: memberId=$memberId, sessionId=${session.id}" }
    }

    fun sendToUser(memberId: Long, message: String) {
        val sessions = userSessions[memberId] ?: return
        val textMessage = TextMessage(message)
        sessions.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(textMessage)
                }
            } catch (e: Exception) {
                log.warn { "알림 전송 실패: memberId=$memberId, error=${e.message}" }
            }
        }
    }

    private fun memberIdOf(session: WebSocketSession): Long? =
        session.attributes[NotificationHandshakeInterceptor.MEMBER_ID] as? Long
}
