package com.example.investfeed.domain.security

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.exception.SECONDARY_PASSWORD_SUSPENDED_MESSAGE
import com.example.investfeed.domain.auth.service.SecondaryPasswordAttemptService
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit

@Component
class SecondaryAuthFilter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    private val protectedPaths = listOf(
        "/api/admin/",                  // 관리자 도메인 전체
        "/api/stock/holdings/",         // 주식 계좌 (HoldingController + ManualHoldingController)
        "/api/crypto/holdings/",        // 코인 계좌
        "/api/asset/",                  // 통합 자산
        "/api/stock/realized-pnl/",     // 주식 실현손익
        "/api/crypto/realized-pnl/",    // 코인 실현손익
        "/api/goals/",                  // 투자 목표
        "/api/rebalancing/",            // 리밸런싱
        "/api/assistant/secure/"        // AI 비서 개인 파트·비서 토큰 (시장 파트는 로그인만)
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI

        if (!requiresSecondaryAuth(requestUri)) {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = (authentication?.principal as? CustomUserDetails) ?: run {
            filterChain.doFilter(request, response)
            return
        }

        val loginId = userDetails.username
        val member = userDetails.member

        if (member.secondaryPassword == null) {
            writeResponse(response, ResponseCode.AUTH_SECONDARY_NOT_SET)
            return
        }

        // 2차 비밀번호를 여러 번 틀려 정지된 회원은 관리자가 초기화할 때까지 보호 기능에 들어갈 수 없다.
        if (SecondaryPasswordAttemptService.isSuspended(member.failedSecondaryAttempts)) {
            log.warn { "2차 비밀번호 정지 상태의 접근: uri=$requestUri, loginId=$loginId" }
            writeSuspendedResponse(response)
            return
        }

        val token = request.cookies
            ?.firstOrNull { it.name == "secondaryAuthToken" }
            ?.value

        val storedToken = redisTemplate.opsForValue().get("${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId")
        if (token == null || storedToken == null || storedToken != token) {
            log.debug { "2차 인증 요구: uri=$requestUri, loginId=$loginId, cookieToken=${if (token == null) "NULL" else "present"}, redisToken=${if (storedToken == null) "NULL" else "present"}, match=${storedToken == token}" }
            writeResponse(response, ResponseCode.AUTH_SECONDARY_REQUIRED)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun requiresSecondaryAuth(uri: String): Boolean {
        return protectedPaths.any { prefix ->
            val base = prefix.trimEnd('/')
            uri == base || uri.startsWith("$base/")
        }
    }

    private fun writeResponse(response: HttpServletResponse, code: ResponseCode) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ApiResponse(code = code.code, message = code.message, result = null)
        )
    }

    private fun writeSuspendedResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ApiResponse(
                code = "AUTH_4044",
                message = SECONDARY_PASSWORD_SUSPENDED_MESSAGE,
                result = null
            )
        )
    }
}
