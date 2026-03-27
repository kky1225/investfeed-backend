package com.example.investfeed.common.exception

import com.example.investfeed.domain.ResponseCode
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
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

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDeniedException(e: AccessDeniedException): ResponseEntity<ApiResponse<Nothing?>> {
        log.warn { "accessDeniedException: ${e.message}" }

        return ResponseEntity(
            ApiResponse(code = ResponseCode.AUTH_FORBIDDEN.code, message = ResponseCode.AUTH_FORBIDDEN.message, null), HttpStatus.FORBIDDEN
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
