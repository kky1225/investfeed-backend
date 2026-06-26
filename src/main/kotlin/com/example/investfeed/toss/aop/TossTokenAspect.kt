package com.example.investfeed.toss.aop

import com.example.investfeed.toss.auth.service.TossAuthClient
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class TossTokenAspect(
    private val tossAuthClient: TossAuthClient
) {
    @Before("@annotation(com.example.investfeed.toss.annotation.TossToken)")
    fun ensureAccessToken() {
        tossAuthClient.accessToken()
    }
}
