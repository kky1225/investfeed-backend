package com.example.investfeed.internal.assistant

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.SecurityPaths
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.assistant.service.AssistantTokenService
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ServiceTokenFilter(
    private val assistantTokenService: AssistantTokenService,
    private val memberRepository: MemberRepository,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    companion object {
        private val CHAT_SCOPES = setOf(AssistantTokenService.SCOPE_CHAT, AssistantTokenService.SCOPE_CHAT_MARKET)

        /** 개인 도구 (Python registry 의 DISPLAY·ACTION). 시장 토큰으로는 호출할 수 없다 — Python 이 먼저 막고 여기서 한 번 더 막는다 */
        private val PERSONAL_TOOL_PREFIXES = listOf(
            "/internal/assistant/tools/show_",
            "/internal/assistant/tools/create_price_alert",
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith(SecurityPaths.INTERNAL_PATTERN.removeSuffix("**"))

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader("Authorization") ?: ""
        if (!header.startsWith("Bearer ")) return reject(response, "bearer token required")
        val token = header.removePrefix("Bearer ").trim()

        val member = try {
            val claims = assistantTokenService.verifyForCoreTools(token)
            val scope = claims["scope"]
            if (scope !in CHAT_SCOPES) return reject(response, "scope must be chat")
            if (scope != AssistantTokenService.SCOPE_CHAT && PERSONAL_TOOL_PREFIXES.any { request.requestURI.startsWith(it) }) {
                return reject(response, "secondary auth required", HttpServletResponse.SC_FORBIDDEN)
            }
            val memberId = claims.subject.toLongOrNull() ?: return reject(response, "sub must be memberId")
            memberRepository.findById(memberId).orElse(null) ?: return reject(response, "unknown member")
        } catch (e: Exception) {
            log.warn { "서비스 토큰 검증 실패: ${e.javaClass.simpleName}: ${e.message}" }
            return reject(response, "invalid service token")
        }

        val details = CustomUserDetails(member)
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(details, null, details.authorities)
        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    private fun reject(response: HttpServletResponse, reason: String, status: Int = HttpServletResponse.SC_UNAUTHORIZED) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ApiResponse(code = ResponseCode.AUTH_UNAUTHORIZED.code, message = "${ResponseCode.AUTH_UNAUTHORIZED.message} ($reason)", result = null)
        )
    }
}
