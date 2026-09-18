package com.example.investfeed.common.security

/**
 * 보안/권한 관련 path 상수.
 *
 * 단일 소스 — SecurityConfig (permitAll), WebMvcConfig (interceptor exclude),
 * PermissionAnnotationValidator (검증 대상 제외) 가 모두 여기를 참조한다.
 *
 * 변경 시 위 3곳 자동 동기화. 직접 path 문자열을 hardcode 하지 말 것.
 *
 * 패턴 표기:
 *  - PATTERN 접미사 — Spring Ant 패턴 (requestMatchers/addPathPatterns 인자)
 *  - PREFIX 접미사  — 단순 prefix (startsWith 검사용)
 */
object SecurityPaths {
    const val API_PREFIX = "/api/"
    const val API_PATTERN = "/api/**"

    // 인증/공개 endpoint — 권한 검증 대상 외 (login/refresh/totp 등).
    const val AUTH_PREFIX = "/api/auth/"
    const val AUTH_PATTERN = "/api/auth/**"

    // WebSocket — 권한 검증 대상 외 (Spring Security permitAll 만 적용).
    const val WS_PATTERN = "/ws/**"

    // AI 비서 JWKS (공개키) — 인증·권한 검증 없음. Python 이 비서 토큰 검증용으로 읽는다.
    const val ASSISTANT_JWKS_PREFIX = "/api/assistant/.well-known/"
    const val ASSISTANT_JWKS_PATTERN = "/api/assistant/.well-known/**"

    // 서비스 간 내부 API — 쿠키 JWT·2차 인증·PermissionInterceptor 제외. ServiceTokenFilter(비서 토큰 RS256) 가 단독 게이트.
    const val INTERNAL_PATTERN = "/internal/**"

    //
    // 인증 후에만 허용되는 auth sub-path 목록.
    // 비밀번호 변경/프로필/API 키 관리/2차 인증 관리/admin sub-path.
    // PermissionInterceptor 까지는 도달하지 않으므로 권한 어노테이션 불필요.
    //
    val AUTH_AUTHENTICATED_PATTERNS = arrayOf(
        "/api/auth/password",
        "/api/auth/profile",
        "/api/auth/api-keys/**",
        "/api/auth/secondary-password/**",
        "/api/auth/admin/**",
    )

    //
    // PermissionAnnotationValidator 가 검증 대상에서 제외할 path prefix 목록.
    // WebMvcConfig 의 excludePathPatterns 와 동기.
    //
    val PERMISSION_EXCLUDED_PREFIXES = listOf(
        AUTH_PREFIX,
        ASSISTANT_JWKS_PREFIX,
    )
}
