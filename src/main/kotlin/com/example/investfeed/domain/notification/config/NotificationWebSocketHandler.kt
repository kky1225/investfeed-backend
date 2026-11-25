package com.example.investfeed.domain.notification.config

import com.example.investfeed.common.util.failure
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationWebSocketHandler : TextWebSocketHandler() {

    companion object {
        // 화면 NotificationContext·reconnectingSocket 과 값을 맞출 것 (시세 WebSocketHandler 와 같은 값)
        private val LOGGED_OUT = CloseStatus(4001, "logged out")

        // 다른 곳에서 새로 로그인한 경우. 화면은 "다른 곳에서 로그인" 안내를 띄우고 다시 붙지 않는다
        private val REPLACED = CloseStatus(4002, "logged in elsewhere")
    }

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
        log.debug { "알림 WebSocket 종료: memberId=$memberId, sessionId=${session.id}" }
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

    /** 로그아웃·정지 시 연결을 닫는다. 화면은 이 코드를 받으면 다시 붙지 않는다 */
    fun closeSessions(memberId: Long) = close(memberId, LOGGED_OUT)

    /** 새로 로그인했을 때 이전 연결을 닫는다 */
    fun closeReplacedSessions(memberId: Long) = close(memberId, REPLACED)

    private fun close(memberId: Long, status: CloseStatus) {
        userSessions[memberId]?.forEach { session ->
            runCatching { session.close(status) }
                .onFailure { log.failure(it) { "알림 세션 종료 실패: memberId=$memberId, sessionId=${session.id}" } }
        }
    }

    private fun memberIdOf(session: WebSocketSession): Long? =
        session.attributes[NotificationHandshakeInterceptor.MEMBER_ID] as? Long
}
