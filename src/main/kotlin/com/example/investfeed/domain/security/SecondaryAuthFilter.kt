package com.example.investfeed.domain.security

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
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
        "/api/rebalancing/"             // 리밸런싱
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

        val token = request.cookies
            ?.firstOrNull { it.name == "secondaryAuthToken" }
            ?.value

        val storedToken = redisTemplate.opsForValue().get("${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId")
        if (token == null || storedToken == null || storedToken != token) {
            val lockTtl = redisTemplate.getExpire("${RedisKeyPrefix.SECONDARY_AUTH_LOCK.prefix}$loginId", TimeUnit.SECONDS)
            if (lockTtl > 0) {
                log.warn { "2차 인증 잠금: uri=$requestUri, loginId=$loginId" }
                writeLockedResponse(response)
                return
            }
            log.info { "2차 인증 요구: uri=$requestUri, loginId=$loginId, cookieToken=${if (token == null) "NULL" else "present"}, redisToken=${if (storedToken == null) "NULL" else "present"}, match=${storedToken == token}" }
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

    private fun writeLockedResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ApiResponse(
                code = "AUTH_4044",
                message = "2차 비밀번호 입력이 잠금되었습니다.",
                result = null
            )
        )
    }
}
