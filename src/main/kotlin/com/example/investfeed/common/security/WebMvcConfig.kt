package com.example.investfeed.common.security

import com.example.investfeed.common.logging.RequestLogInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val permissionInterceptor: PermissionInterceptor,
    private val requestLogInterceptor: RequestLogInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 요청 성공 로그 1건 — 화면 API·비서 내부 API 전부
        registry.addInterceptor(requestLogInterceptor)
            .addPathPatterns(SecurityPaths.API_PATTERN, SecurityPaths.INTERNAL_PATTERN)

        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns(SecurityPaths.API_PATTERN)
            .excludePathPatterns(
                SecurityPaths.AUTH_PATTERN,
                SecurityPaths.ASSISTANT_JWKS_PATTERN,
            )
    }
}
