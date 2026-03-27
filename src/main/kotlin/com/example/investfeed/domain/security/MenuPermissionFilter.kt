package com.example.investfeed.domain.security

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.menu.service.MenuService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class MenuPermissionFilter(
    private val menuService: MenuService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    private val excludedPaths = listOf("/api/auth/", "/api/admin/", "/api/menus/", "/ws/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI

        if (shouldSkip(requestUri)) {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated) {
            val userDetails = authentication.principal as? CustomUserDetails

            if (userDetails != null) {
                val role = userDetails.member.role

                try {
                    menuService.checkMenuAccess(requestUri, role)
                } catch (e: AccessDeniedException) {
                    log.error { "Menu access denied: $requestUri for role: ${role.name}" }
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    objectMapper.writeValue(
                        response.writer,
                        ApiResponse(code = ResponseCode.AUTH_FORBIDDEN.code, message = e.message ?: ResponseCode.AUTH_FORBIDDEN.message, result = null)
                    )
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun shouldSkip(uri: String): Boolean =
        excludedPaths.any { uri.startsWith(it) }
}
