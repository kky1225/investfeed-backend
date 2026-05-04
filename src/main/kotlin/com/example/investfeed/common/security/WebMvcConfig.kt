package com.example.investfeed.common.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val permissionInterceptor: PermissionInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns(SecurityPaths.API_PATTERN)
            .excludePathPatterns(
                SecurityPaths.AUTH_PATTERN,
            )
    }
}