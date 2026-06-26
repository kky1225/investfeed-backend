package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.service.HoldingService
import com.example.investfeed.domain.holding.service.TossHoldingService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.STOCK_HOLDINGS)
@RestController
@RequestMapping("/api/stock/holdings")
class HoldingController(
    private val holdingService: HoldingService,
    private val tossHoldingService: TossHoldingService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listHoldings(): ResponseEntity<ApiResponse<HoldingListRes>> {
        log.info { "listHoldings" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_LIST.code,
                message = ResponseCode.HOLDING_LIST.message,
                result = holdingService.listHoldings()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/toss")
    @RequiresAction(action = Actions.READ)
    fun listTossHoldings(): ResponseEntity<ApiResponse<HoldingListRes>> {
        log.info { "listTossHoldings" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_LIST.code,
                message = ResponseCode.HOLDING_LIST.message,
                result = tossHoldingService.listTossHoldings()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamHoldings(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamHoldings $req" }

        holdingService.streamHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_STREAM.code,
                message = ResponseCode.HOLDING_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
