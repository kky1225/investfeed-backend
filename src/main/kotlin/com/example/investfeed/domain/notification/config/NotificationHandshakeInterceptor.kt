package com.example.investfeed.domain.notification.config

import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.security.JwtProvider
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class NotificationHandshakeInterceptor(
    private val jwtProvider: JwtProvider
) : HandshakeInterceptor {

    private val log = KotlinLogging.logger {}

    companion object {
        const val MEMBER_ID = "memberId"
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val memberId = extractMemberId(request)
        if (memberId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            log.debug { "알림 WebSocket 인증 실패 - 핸드셰이크 거절" }
            return false
        }
        attributes[MEMBER_ID] = memberId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun extractMemberId(request: ServerHttpRequest): Long? {
        return try {
            val token = request.headers["Cookie"]?.firstOrNull()
                ?.split(";")
                ?.map { it.trim() }
                ?.find { it.startsWith("accessToken=") }
                ?.substringAfter("accessToken=")
                ?: return null

            if (!jwtProvider.validateToken(token)) return null

            val userDetails = jwtProvider.getAuthentication(token).principal as CustomUserDetails
            userDetails.member.id
        } catch (e: Exception) {
            log.debug { "알림 WebSocket 토큰 파싱 실패: ${e.message}" }
            null
        }
    }
}
