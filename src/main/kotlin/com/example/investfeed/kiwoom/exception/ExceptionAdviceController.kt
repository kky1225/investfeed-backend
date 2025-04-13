package com.example.investfeed.kiwoom.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionAdviceController {

    @ExceptionHandler(Exception::class)
    fun exception(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        return ResponseEntity(
            ApiResponse(code = "S9999", message = "서버 에러", null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun runtimeException(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        return ResponseEntity(
            ApiResponse(code = "S9999", message = "서버 에러", null), HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}