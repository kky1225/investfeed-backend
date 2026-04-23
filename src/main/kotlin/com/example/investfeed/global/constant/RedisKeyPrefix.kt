package com.example.investfeed.global.constant

/**
 * 프로젝트에서 사용하는 Redis key prefix 목록.
 *
 * 각 서비스는 이 enum 을 참조하여 key 를 생성하고,
 * 모니터링 페이지는 `values()` 로 전체 prefix 를 순회해 캐시 현황을 집계한다.
 *
 * 새로운 Redis 캐시/상태가 추가되면 반드시 이 enum 에 항목을 추가해야
 * 관리자 페이지에서 자동으로 추적된다.
 */
enum class RedisKeyPrefix(
    val prefix: String,
    val description: String,
) {
    ECONOMIC_CALENDAR("ECON:", "경제 캘린더 캐시"),
    MARKET_INDEX("market-index:", "주요 시장 지수 캐시"),
    NEWS("NEWS:", "네이버 뉴스 캐시"),
    INVESTOR_CLOSE_MARKET("investor:closeMarket:", "투자자별 장마감 데이터 캐시"),
    KIWOOM_ACCESS_TOKEN("kiwoom:access_token:", "키움 OAuth access token"),
    KIWOOM_ACCESS_TOKEN_LOCK("lock:kiwoom:access_token:", "키움 OAuth access token 갱신 락"),
    PRE_AUTH_TOKEN("PRE_AUTH:", "TOTP 등록 전 임시 토큰"),
    PRE_AUTH_SECRET("PRE_AUTH_SECRET:", "TOTP 등록 전 임시 secret"),
    SECONDARY_AUTH("SEC_AUTH:", "관리자 2차 인증 세션"),
    SECONDARY_AUTH_LOCK("SEC_LOCK:", "관리자 2차 인증 실패 잠금"),
    SECONDARY_AUTH_FAIL("SEC_FAIL:", "관리자 2차 인증 실패 카운터"),
    REFRESH_TOKEN("RT:", "JWT refresh token"),
    BLACKLIST("BL:", "JWT access token 블랙리스트"),
    TOTP_SECRET("TOTP:", "TOTP 비밀키"),
}
