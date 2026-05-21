package com.example.investfeed.kiwoom.aop

import com.example.investfeed.kiwoom.auth.service.AuthClient
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

/**
 * @KiwoomMockToken 메서드 실행 전 모의투자 도메인 토큰을 보장한다.
 * 실거래용 [KiwoomTokenAspect] 와 분리 — 모의 클라이언트만 이 경로를 탄다.
 */
@Aspect
@Component
class KiwoomMockTokenAspect(
    private val authClient: AuthClient
) {
    @Before("@annotation(com.example.investfeed.kiwoom.annotation.KiwoomMockToken)")
    fun ensureAccessTokenMock() {
        authClient.accessTokenMock()
    }
}