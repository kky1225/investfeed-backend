package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.service.CryptoHoldingService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/crypto/holding")
class CryptoHoldingController(
    private val cryptoHoldingService: CryptoHoldingService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun cryptoHoldingList(): ResponseEntity<ApiResponse<HoldingListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_LIST.code,
                message = ResponseCode.CRYPTO_HOLDING_LIST.message,
                result = cryptoHoldingService.cryptoHoldingList()
            ), HttpStatus.OK
        )
    }

    @PostMapping("stream")
    fun cryptoHoldingStream(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoHoldingService.cryptoHoldingStream(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_STREAM.code,
                message = ResponseCode.CRYPTO_HOLDING_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("reorder")
    fun reorderHoldings(
        @RequestBody req: HoldingReorderReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoHoldingService.reorderHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_REORDER.code,
                message = ResponseCode.CRYPTO_HOLDING_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
