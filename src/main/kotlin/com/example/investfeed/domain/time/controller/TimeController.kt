package com.example.investfeed.domain.time.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.time.dto.req.TimeNowReq
import com.example.investfeed.domain.time.dto.res.TimeNowRes
import com.example.investfeed.domain.time.service.TimeService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/time")
class TimeController(
    private val timeService: TimeService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("now")
    fun getCurrentTime(
        req: TimeNowReq
    ): ResponseEntity<ApiResponse<TimeNowRes>> {
        log.info { "getCurrentTime: $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.TIME_NOW.code,
                message = ResponseCode.TIME_NOW.message,
                result = timeService.getCurrentTime(
                    req = req
                )
            ), HttpStatus.OK
        )
    }
}
