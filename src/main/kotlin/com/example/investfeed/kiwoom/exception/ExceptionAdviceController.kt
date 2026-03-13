package com.example.investfeed.kiwoom.exception

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @ExceptionHandler(InvestFeedException::class)
    fun investFeedException(e: InvestFeedException): ResponseEntity<ApiResponse<Nothing?>> {
        log.error { "investFeedException $e" }

        return ResponseEntity(
            ApiResponse(code = e.code, message = e.message, null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(AuthException::class)
    fun authException(e: AuthException): ResponseEntity<ApiResponse<Nothing?>> {
        log.warn { "authException: [${e.code}] ${e.message}" }

        return ResponseEntity(
            ApiResponse(code = e.code, message = e.message, null), HttpStatus.BAD_REQUEST
        )
    }
}