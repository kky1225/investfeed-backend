package com.example.investfeed.common.exception

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.dto.res.LoginErrorRes
import com.example.investfeed.domain.auth.dto.res.SecondaryPasswordLockStatusRes
import com.example.investfeed.domain.auth.exception.AccountLockedByFailureException
import com.example.investfeed.domain.auth.exception.AccountLockedException
import com.example.investfeed.domain.auth.exception.AccountPermanentlyLockedException
import com.example.investfeed.domain.auth.exception.SecondaryPasswordLockedException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionAdviceController {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(Exception::class)
    fun exception(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        log.error { "exception $e" }

        return ResponseEntity(
            ApiResponse(code = "SERVER_9999", message = "서버 에러", null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun runtimeException(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        log.error { "runtimeException $e" }

        return ResponseEntity(
            ApiResponse(code = "SERVER_9999", message = "서버 에러", null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun authenticationException(e: AuthenticationException): ResponseEntity<ApiResponse<*>> {
        log.warn { "authenticationException: ${e.message}" }

        val (code, result) = when (e) {
            is AccountLockedByFailureException -> "AUTH_4012" to LoginErrorRes(e.lockRemainingSeconds)
            is AccountLockedException -> "AUTH_4013" to LoginErrorRes(e.lockRemainingSeconds)
            is AccountPermanentlyLockedException -> "AUTH_4014" to null
            else -> "AUTH_4011" to null
        }

        return ResponseEntity(
            ApiResponse(code = code, message = e.message ?: "인증에 실패하였습니다.", result), HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDeniedException(e: AccessDeniedException): ResponseEntity<ApiResponse<*>> {
        log.warn { "accessDeniedException: ${e.message}" }

        val (code, result) = when (e) {
            is SecondaryPasswordLockedException -> "AUTH_4044" to SecondaryPasswordLockStatusRes(e.remainingSeconds)
            else -> ResponseCode.AUTH_FORBIDDEN.code to null
        }

        return ResponseEntity(
            ApiResponse(code = code, message = e.message ?: "접근 권한이 없습니다.", result), HttpStatus.FORBIDDEN
        )
    }

    @ExceptionHandler(InvestFeedException::class)
    fun investFeedException(e: InvestFeedException): ResponseEntity<ApiResponse<Nothing?>> {
        log.error { "investFeedException: [${e.code}] ${e.message}" }

        return ResponseEntity(
            ApiResponse(code = e.code, message = e.message, null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
