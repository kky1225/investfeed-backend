package com.example.investfeed.domain.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.cookies?.find { it.name == "accessToken" }?.value

        if (token != null && jwtProvider.validateToken(token) && !jwtProvider.isBlacklisted(token)) {
            val authentication = jwtProvider.getAuthentication(token)
            // 관리자가 정지한 계정은 access 토큰이 남아 있어도 인증하지 않는다
            if (!(authentication.principal as CustomUserDetails).member.isSuspended()) {
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
