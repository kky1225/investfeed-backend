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

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith(SecurityPaths.INTERNAL_PATTERN.removeSuffix("**"))

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val header = request.getHeader("Authorization") ?: ""
        if (!header.startsWith("Bearer ")) return reject(response, "bearer token required")
        val token = header.removePrefix("Bearer ").trim()

        val member = try {
            val claims = assistantTokenService.verifyForCoreTools(token)
            if (claims["scope"] != AssistantTokenService.SCOPE_CHAT) return reject(response, "scope must be chat")
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

    private fun reject(response: HttpServletResponse, reason: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(
            response.writer,
            ApiResponse(code = ResponseCode.AUTH_UNAUTHORIZED.code, message = "${ResponseCode.AUTH_UNAUTHORIZED.message} ($reason)", result = null)
        )
    }
}
