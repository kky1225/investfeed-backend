package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.HoldingListRes
import com.example.investfeed.domain.holding.service.CryptoHoldingService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.CRYPTO_HOLDINGS)
@RestController
@RequestMapping("/api/crypto/holdings")
class CryptoHoldingController(
    private val cryptoHoldingService: CryptoHoldingService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listCryptoHoldings(): ResponseEntity<ApiResponse<HoldingListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_LIST.code,
                message = ResponseCode.CRYPTO_HOLDING_LIST.message,
                result = cryptoHoldingService.listCryptoHoldings()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamCryptoHoldings(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoHoldingService.streamCryptoHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_STREAM.code,
                message = ResponseCode.CRYPTO_HOLDING_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderCryptoHoldings(
        @RequestBody req: HoldingReorderReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoHoldingService.reorderCryptoHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_REORDER.code,
                message = ResponseCode.CRYPTO_HOLDING_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
