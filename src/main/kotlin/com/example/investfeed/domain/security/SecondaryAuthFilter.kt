package com.example.investfeed.domain.security

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

    private val protectedPaths = listOf("/api/auth/admin/", "/api/admin/", "/api/stock/holding/", "/api/crypto/holding/", "/api/asset/")

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

        val storedToken = redisTemplate.opsForValue().get("SEC_AUTH:$loginId")
        if (token == null || storedToken == null || storedToken != token) {
            val lockTtl = redisTemplate.getExpire("SEC_LOCK:$loginId", TimeUnit.SECONDS)
            if (lockTtl > 0) {
                writeLockedResponse(response)
                return
            }
            writeResponse(response, ResponseCode.AUTH_SECONDARY_REQUIRED)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun requiresSecondaryAuth(uri: String): Boolean =
        protectedPaths.any { uri.startsWith(it) }

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
