package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.service.HoldingService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/stock/holding")
class HoldingController(
    private val holdingService: HoldingService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun holdingList(): ResponseEntity<ApiResponse<HoldingListRes>> {
        log.info { "holdingList" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_LIST.code,
                message = ResponseCode.HOLDING_LIST.message,
                result = holdingService.holdingList()
            ), HttpStatus.OK
        )
    }

    @PostMapping("stream")
    fun holdingStream(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "holdingStream $req" }

        holdingService.holdingStream(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_STREAM.code,
                message = ResponseCode.HOLDING_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
