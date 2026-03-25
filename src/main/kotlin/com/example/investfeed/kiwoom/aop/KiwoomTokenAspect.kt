package com.example.investfeed.kiwoom.aop

import com.example.investfeed.kiwoom.auth.service.AuthClient
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class KiwoomTokenAspect(
    private val authClient: AuthClient
) {
    @Before("@annotation(com.example.investfeed.kiwoom.annotation.KiwoomToken)")
    fun ensureAccessToken() {
        authClient.accessToken()
    }
}
