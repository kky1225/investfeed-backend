package com.example.investfeed.kiwoom.time.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.time.dto.TimeNowRes
import com.example.investfeed.kiwoom.time.service.TimeService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(("/api/time"))
class TimeController(
    private val timeService: TimeService
) {
    private val log = KotlinLogging.logger {}


    @GetMapping("/now")
    fun timeNow(): ResponseEntity<ApiResponse<TimeNowRes>> {
        log.info { "timeNow" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.TIME_NOW.code,
                message = ResponseCode.TIME_NOW.message,
                result = timeService.timeNow()
            ), HttpStatus.OK
        )
    }
}