package com.example.investfeed.domain.notification.config

import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.security.JwtProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificationWebSocketHandler(
    private val jwtProvider: JwtProvider
) : TextWebSocketHandler() {

    private val log = KotlinLogging.logger {}
    private val userSessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val memberId = extractMemberId(session)
        if (memberId == null) {
            log.warn { "알림 WebSocket 인증 실패 - 연결 종료" }
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }

        userSessions.computeIfAbsent(memberId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val memberId = extractMemberId(session)
        if (memberId != null) {
            userSessions[memberId]?.remove(session)
            if (userSessions[memberId]?.isEmpty() == true) {
                userSessions.remove(memberId)
            }
            log.info { "알림 WebSocket 종료: memberId=$memberId, sessionId=${session.id}" }
        }
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
                log.error { "알림 전송 실패: memberId=$memberId, error=${e.message}" }
            }
        }
    }

    private fun extractMemberId(session: WebSocketSession): Long? {
        return try {
            val cookieHeader = session.handshakeHeaders["Cookie"]?.firstOrNull() ?: return null
            val token = cookieHeader.split(";")
                .map { it.trim() }
                .find { it.startsWith("accessToken=") }
                ?.substringAfter("accessToken=") ?: return null

            if (!jwtProvider.validateToken(token)) return null

            val authentication = jwtProvider.getAuthentication(token)
            val userDetails = authentication.principal as CustomUserDetails
            userDetails.member.id
        } catch (e: Exception) {
            log.error { "WebSocket 토큰 파싱 실패: ${e.message}" }
            null
        }
    }
}
