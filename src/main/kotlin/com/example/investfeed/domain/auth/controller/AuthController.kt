package com.example.investfeed.domain.auth.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.ChangePasswordReq
import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.service.AuthService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.kiwoom.exception.ApiResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    @param:Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
    private val authService: AuthService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/signup")
    fun signup(@RequestBody req: SignupReq): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "signup: ${req.loginId}" }

        authService.signup(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_SIGNUP.code,
                message = ResponseCode.AUTH_SIGNUP.message,
                result = null
            ), HttpStatus.CREATED
        )
    }

    @PostMapping("/login")
    fun login(
        @RequestBody req: LoginReq,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<TokenRes>> {
        log.info { "login: ${req.loginId}" }

        val (tokenRes, refreshToken) = authService.login(req)
        response.addCookie(createRefreshTokenCookie(refreshToken))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGIN.code,
                message = ResponseCode.AUTH_LOGIN.message,
                result = tokenRes
            ), HttpStatus.OK
        )
    }

    @PostMapping("/reissue")
    fun reissue(
        @CookieValue(name = "refreshToken", required = false) refreshToken: String?
    ): ResponseEntity<ApiResponse<TokenRes>> {
        log.info { "reissue: ${refreshToken}" }

        val tokenRes = authService.reissue(refreshToken)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_REISSUE.code,
                message = ResponseCode.AUTH_REISSUE.message,
                result = tokenRes
            ), HttpStatus.OK
        )
    }

    @PutMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody req: ChangePasswordReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        authService.changePassword(userDetails.username, req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_CHANGE_PASSWORD.code,
                message = ResponseCode.AUTH_CHANGE_PASSWORD.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "logout: ${userDetails.username}" }

        authService.logout(userDetails.username)
        response.addCookie(expireRefreshTokenCookie())

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGOUT.code,
                message = ResponseCode.AUTH_LOGOUT.message,
                result = null
            ), HttpStatus.OK
        )
    }

    private fun createRefreshTokenCookie(refreshToken: String): Cookie {
        var age = refreshTokenExpiration * 24 * 60 * 60

        return Cookie("refreshToken", refreshToken).apply {
            isHttpOnly = true
            secure = true
            path = "/api/auth"
            maxAge = age.toInt()
        }
    }

    private fun expireRefreshTokenCookie(): Cookie {
        return Cookie("refreshToken", "").apply {
            isHttpOnly = true
            secure = true
            path = "/api/auth"
            maxAge = 0
        }
    }
}
