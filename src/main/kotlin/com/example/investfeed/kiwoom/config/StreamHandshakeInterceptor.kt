package com.example.investfeed.kiwoom.config

import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.security.JwtProvider
import mu.KotlinLogging
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * /ws 연결의 계정을 access 쿠키로 식별해 세션에 남긴다. 키움 시세는 이 계정의 세션에만 전달된다.
 * 화면은 시세 등록 REST(만료 시 재발급)가 끝난 뒤 소켓을 열므로 연결 시점의 access 쿠키는 유효하다.
 * 식별되지 않아도 연결은 허용한다 (업비트 시세는 공개 데이터라 전체 전송).
 */
@Component
class StreamHandshakeInterceptor(
    private val jwtProvider: JwtProvider,
) : HandshakeInterceptor {

    private val log = KotlinLogging.logger {}

    companion object {
        const val LOGIN_ID = "loginId"
        private const val ACCESS_TOKEN_COOKIE = "accessToken"
    }

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: org.springframework.web.socket.WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        extractLoginId(request)?.let { attributes[LOGIN_ID] = it }
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: org.springframework.web.socket.WebSocketHandler,
        exception: Exception?,
    ) = Unit

    private fun extractLoginId(request: ServerHttpRequest): String? {
        return try {
            request.headers["Cookie"].orEmpty()
                .flatMap { it.split(";") }
                .map { it.trim() }
                .find { it.startsWith("$ACCESS_TOKEN_COOKIE=") }
                ?.substringAfter("$ACCESS_TOKEN_COOKIE=")
                ?.takeIf { jwtProvider.validateToken(it) && !jwtProvider.isBlacklisted(it) }
                ?.let { (jwtProvider.getAuthentication(it).principal as CustomUserDetails).member }
                ?.takeUnless { it.isSuspended() }
                ?.loginId
        } catch (e: Exception) {
            log.error(e) { "실시간 WebSocket 계정 식별 실패" }
            null
        }
    }
}
