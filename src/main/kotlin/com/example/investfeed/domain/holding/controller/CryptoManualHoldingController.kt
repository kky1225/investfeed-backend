package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingCreateReq
import com.example.investfeed.domain.holding.dto.req.MemberBrokerBalanceUpdateReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingUpdateReq
import com.example.investfeed.domain.holding.dto.res.ManualHoldingItem
import com.example.investfeed.domain.holding.dto.res.ManualHoldingListRes
import com.example.investfeed.domain.holding.service.CryptoManualHoldingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/crypto/holding/manual")
class CryptoManualHoldingController(
    private val cryptoManualHoldingService: CryptoManualHoldingService
) {

    @GetMapping("list/{brokerId}")
    fun manualHoldingList(
        @PathVariable brokerId: Long
    ): ResponseEntity<ApiResponse<ManualHoldingListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MANUAL_HOLDING_LIST.code,
                message = ResponseCode.CRYPTO_MANUAL_HOLDING_LIST.message,
                result = cryptoManualHoldingService.manualHoldingList(brokerId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("create")
    fun createManualHolding(
        @Valid @RequestBody req: ManualHoldingCreateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MANUAL_HOLDING_CREATE.code,
                message = ResponseCode.CRYPTO_MANUAL_HOLDING_CREATE.message,
                result = cryptoManualHoldingService.createManualHolding(req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("{holdingId}")
    fun updateManualHolding(
        @PathVariable holdingId: Long,
        @Valid @RequestBody req: ManualHoldingUpdateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MANUAL_HOLDING_UPDATE.code,
                message = ResponseCode.CRYPTO_MANUAL_HOLDING_UPDATE.message,
                result = cryptoManualHoldingService.updateManualHolding(holdingId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("{holdingId}")
    fun deleteManualHolding(
        @PathVariable holdingId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoManualHoldingService.deleteManualHolding(holdingId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MANUAL_HOLDING_DELETE.code,
                message = ResponseCode.CRYPTO_MANUAL_HOLDING_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("balance/{memberBrokerId}")
    fun updateBalance(
        @PathVariable memberBrokerId: Long,
        @RequestBody req: MemberBrokerBalanceUpdateReq
    ): ResponseEntity<ApiResponse<Long>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MEMBER_BROKER_BALANCE_UPDATE.code,
                message = ResponseCode.CRYPTO_MEMBER_BROKER_BALANCE_UPDATE.message,
                result = cryptoManualHoldingService.updateBalance(memberBrokerId, req.balance)
            ), HttpStatus.OK
        )
    }

    @PutMapping("reorder")
    fun reorderHoldings(
        @RequestBody req: HoldingReorderReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoManualHoldingService.reorderHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_HOLDING_REORDER.code,
                message = ResponseCode.CRYPTO_HOLDING_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
