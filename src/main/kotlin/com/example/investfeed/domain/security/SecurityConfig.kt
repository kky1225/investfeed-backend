package com.example.investfeed.domain.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.SecurityPaths
import com.example.investfeed.domain.ResponseCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    @param:Value("\${cors.allowed-origins}")
    private val allowedOrigins: String,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val secondaryAuthorizationManager: SecondaryAuthorizationManager,
    private val serviceTokenFilter: com.example.investfeed.internal.assistant.ServiceTokenFilter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { auth ->
                // 2차 인증 규칙을 맨 앞에 둔다. 먼저 맞는 규칙 하나만 적용되므로 뒤에 두면 아래 authenticated()·permitAll() 에 가려진다
                auth.requestMatchers(*SecurityPaths.SECONDARY_AUTH_PATTERNS).access(secondaryAuthorizationManager)
                SecurityPaths.SECONDARY_AUTH_WRITE_PATTERNS.forEach { (pattern, methods) ->
                    methods.forEach { method -> auth.requestMatchers(method, pattern).access(secondaryAuthorizationManager) }
                }
                auth
                    .requestMatchers(*SecurityPaths.AUTH_AUTHENTICATED_PATTERNS).authenticated()
                    .requestMatchers(SecurityPaths.AUTH_PATTERN).permitAll()
                    .requestMatchers(SecurityPaths.WS_PATTERN).permitAll()
                    .requestMatchers(SecurityPaths.ASSISTANT_JWKS_PATTERN).permitAll()
                    .requestMatchers(SecurityPaths.INTERNAL_PATTERN).permitAll()   // ServiceTokenFilter 가 401 처리
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, _ ->
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResponseCode.AUTH_UNAUTHORIZED.code, ResponseCode.AUTH_UNAUTHORIZED.message)
                }
                // 2차 인증 거부는 이유별 코드로 답한다 (화면이 설정 창·인증 창·정지 안내를 고른다)
                ex.accessDeniedHandler { request: HttpServletRequest, response: HttpServletResponse, _ ->
                    val code = SecondaryAuthorizationManager.denialOf(request)?.responseCode ?: ResponseCode.AUTH_FORBIDDEN
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, code.code, code.message)
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(serviceTokenFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(response.writer, ApiResponse(code = code, message = message, result = null))
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = allowedOrigins.split(",").map(String::trim)
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = origins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
