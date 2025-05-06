package com.example.investfeed.kiwoom.aop

import com.example.investfeed.kiwoom.auth.service.AuthService
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class KiwoomTokenAspect(
    private val authService: AuthService
) {
    @Before("@annotation(com.example.investfeed.kiwoom.annotation.KiwoomToken)")
    fun accessToken() {
        authService.accessToken()
    }
}