package com.example.investfeed.common.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 화면·비서 요청의 성공 로그는 여기서만 1건 남긴다 (컨트롤러·서비스에서 성공 로그를 남기지 않음).
 * - 변경 요청(POST·PUT·PATCH·DELETE) 성공: info — "POST /api/admin/members/kim/lock 200 35ms user=admin"
 * - 조회(GET 등) 성공: debug
 * - 실패(4xx·5xx·예외): 남기지 않는다 — 실패는 ExceptionAdviceController 가 1건 기록
 * 쿼리 문자열·요청 본문은 남기지 않는다 (개인 정보·키가 섞일 수 있음).
 */
@Component
class RequestLogInterceptor : HandlerInterceptor {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val STARTED_AT_ATTR = "requestLog.startedAt"
        private val MUTATING_METHODS = setOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE).map { it.name() }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(STARTED_AT_ATTR, System.nanoTime())
        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        if (ex != null || response.status >= 400) return

        val startedAt = request.getAttribute(STARTED_AT_ATTR) as? Long ?: return
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        val message = "${request.method} ${request.requestURI} ${response.status} ${elapsedMs}ms user=${currentUser()}"
        if (request.method in MUTATING_METHODS) log.info { message } else log.debug { message }
    }

    private fun currentUser(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth == null || auth is AnonymousAuthenticationToken) "-" else auth.name
    }
}
