package com.example.investfeed.common.security

import com.example.investfeed.domain.permission.repository.PermissionRepository
import com.example.investfeed.domain.permission.repository.RolePermissionRepository
import com.example.investfeed.domain.security.CustomUserDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class PermissionInterceptor(
    private val permissionResolver: PermissionResolver,
    private val permissionRepository: PermissionRepository,
    private val rolePermissionRepository: RolePermissionRepository,
) : HandlerInterceptor {

    private val log = KotlinLogging.logger {}

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 1. controller 메소드 외엔 통과 (정적 리소스, error 페이지 등)
        if (handler !is HandlerMethod) return true

        // 2. 인증 필수. 정상 흐름에선 SecurityConfig 가 미인증을 401 로 차단하므로 여기 도달 시 항상 인증된 상태.
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("인증 정보가 없습니다.")
        val userDetails = auth.principal as? CustomUserDetails
            ?: throw AccessDeniedException("인증 정보가 올바르지 않습니다.")

        val role = userDetails.member.role
        val uri = request.requestURI
        val method = request.method

        // 3. (permission code, action) — controller annotation 으로부터 추출
        //    HTTP method 컨벤션 fallback 제거됨 — 어노테이션 누락 시 거부
        val resolved = permissionResolver.resolve(handler)
            ?: run {
                log.error { "API access denied (no @RequiresAction(action) on method or class): $method $uri for role: ${role.code}" }
                throw AccessDeniedException("권한 명시가 없는 API 입니다.")
            }
        val (permissionCode, action) = resolved

        // 4. permission entity 조회 (없으면 시드 누락 — 데이터 정합성 문제)
        val permission = permissionRepository.findByCode(permissionCode)
            ?: run {
                log.error { "Permission code '$permissionCode' not found in catalog (seed missing)" }
                throw AccessDeniedException("등록되지 않은 권한입니다: $permissionCode")
            }

        val granted = rolePermissionRepository.existsByRoleIdAndPermissionIdAndAction(
            role.id, permission.id, action
        )
        if (!granted) {
            log.error { "API access denied: $method $uri (need $permissionCode.$action) for role: ${role.code}" }
            throw AccessDeniedException("해당 API 에 대한 권한이 없습니다.")
        }

        return true
    }
}
