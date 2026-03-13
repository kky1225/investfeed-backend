package com.example.investfeed.domain.auth.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.ReissueReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.service.AuthService
import com.example.investfeed.kiwoom.config.security.CustomUserDetails
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/signup")
    fun signup(@RequestBody req: SignupReq): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "signup: ${req.email}" }
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
    fun login(@RequestBody req: LoginReq): ResponseEntity<ApiResponse<TokenRes>> {
        log.info { "login: ${req.email}" }
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGIN.code,
                message = ResponseCode.AUTH_LOGIN.message,
                result = authService.login(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/reissue")
    fun reissue(@RequestBody req: ReissueReq): ResponseEntity<ApiResponse<TokenRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_REISSUE.code,
                message = ResponseCode.AUTH_REISSUE.message,
                result = authService.reissue(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/logout")
    fun logout(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "logout: ${userDetails.username}" }
        authService.logout(userDetails.username)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.AUTH_LOGOUT.code,
                message = ResponseCode.AUTH_LOGOUT.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
