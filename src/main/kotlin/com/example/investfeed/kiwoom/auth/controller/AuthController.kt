package com.example.investfeed.kiwoom.auth.controller

import com.example.investfeed.kiwoom.auth.model.AccessTokenRes
import com.example.investfeed.kiwoom.auth.service.AuthService
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("api/auth")
@RestController
class AuthController(
    private val authService: AuthService,
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("accessToken")
    fun accessToken(): ResponseEntity<ApiResponse<AccessTokenRes?>> {
        log.info { "accessToken" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ACCESS_TOKEN.code,
                message = ResponseCode.ACCESS_TOKEN.message,
                authService.accessToken()
            ), HttpStatus.OK
        )
    }
}