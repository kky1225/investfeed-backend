package com.example.investfeed.common.security

import org.springframework.http.HttpMethod

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
    // 2차 인증(2차 비밀번호)이 필요한 경로. SecurityConfig 가 SecondaryAuthorizationManager 로 검사한다.
    // 주소를 옮기면 2차 인증 대상에서 빠지므로 이 목록을 함께 고칠 것.
    //
    val SECONDARY_AUTH_PATTERNS = arrayOf(
        "/api/admin/**",                  // 관리자 도메인 전체
        "/api/stock/holdings/**",         // 주식 계좌 (보유·직접 입력·매매)
        "/api/crypto/holdings/**",        // 코인 계좌
        "/api/asset/**",                  // 통합 자산
        "/api/stock/realized-pnl/**",     // 주식 실현손익
        "/api/crypto/realized-pnl/**",    // 코인 실현손익
        "/api/goals/**",                  // 투자 목표
        "/api/rebalancing/**",            // 리밸런싱
        "/api/assistant/secure/**",       // AI 비서 개인 파트·비서 토큰 (시장 파트는 로그인만)
    )

    //
    // 조회와 변경이 같은 주소를 쓰는 곳. 조회는 로그인만으로 열어 두고 변경 요청만 2차 인증을 거친다.
    //
    val SECONDARY_AUTH_WRITE_PATTERNS: List<Pair<String, Set<HttpMethod>>> = listOf(
        "/api/auth/api-keys/**" to setOf(HttpMethod.POST, HttpMethod.DELETE),                       // 증권사 API 키 등록·삭제 (목록은 앞 4자리만 보인다)
        "/api/assistant/telegram/**" to setOf(HttpMethod.POST, HttpMethod.PATCH, HttpMethod.DELETE), // 텔레그램 연결 코드 발급·켜고 끄기·연결 해제
        "/api/stock/brokers/my/**" to setOf(HttpMethod.POST, HttpMethod.DELETE),                    // 내 증권사 추가·삭제 (삭제하면 그 증권사 보유 종목도 지워진다)
        "/api/crypto/brokers/my/**" to setOf(HttpMethod.POST, HttpMethod.DELETE),                   // 내 거래소 추가·삭제
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
