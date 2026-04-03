package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingCreateReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingUpdateReq
import com.example.investfeed.domain.holding.dto.res.ManualHoldingItem
import com.example.investfeed.domain.holding.dto.res.ManualHoldingListRes
import com.example.investfeed.domain.holding.service.ManualHoldingService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/stock/holding/manual")
class ManualHoldingController(
    private val manualHoldingService: ManualHoldingService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list/{brokerId}")
    fun manualHoldingList(
        @PathVariable brokerId: Long
    ): ResponseEntity<ApiResponse<ManualHoldingListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_LIST.code,
                message = ResponseCode.MANUAL_HOLDING_LIST.message,
                result = manualHoldingService.manualHoldingList(brokerId)
            ), HttpStatus.OK
        )
    }

    @PostMapping("create")
    fun createManualHolding(
        @RequestBody req: ManualHoldingCreateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_CREATE.code,
                message = ResponseCode.MANUAL_HOLDING_CREATE.message,
                result = manualHoldingService.createManualHolding(req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("{holdingId}")
    fun updateManualHolding(
        @PathVariable holdingId: Long,
        @RequestBody req: ManualHoldingUpdateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_UPDATE.code,
                message = ResponseCode.MANUAL_HOLDING_UPDATE.message,
                result = manualHoldingService.updateManualHolding(holdingId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("{holdingId}")
    fun deleteManualHolding(
        @PathVariable holdingId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        manualHoldingService.deleteManualHolding(holdingId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_DELETE.code,
                message = ResponseCode.MANUAL_HOLDING_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PutMapping("reorder")
    fun reorderHoldings(
        @RequestBody req: HoldingReorderReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        manualHoldingService.reorderHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_REORDER.code,
                message = ResponseCode.HOLDING_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
