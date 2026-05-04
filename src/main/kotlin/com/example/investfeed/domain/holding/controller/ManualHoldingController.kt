package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingReorderReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingCreateReq
import com.example.investfeed.domain.holding.dto.req.MemberBrokerBalanceUpdateReq
import com.example.investfeed.domain.holding.dto.req.ManualHoldingUpdateReq
import com.example.investfeed.domain.holding.dto.res.ManualHoldingItem
import com.example.investfeed.domain.holding.dto.res.ManualHoldingListRes
import com.example.investfeed.domain.holding.service.ManualHoldingService
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.STOCK_HOLDINGS)
@RestController
@RequestMapping("/api/stock/holdings/manual")
class ManualHoldingController(
    private val manualHoldingService: ManualHoldingService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("/{brokerId}")
    @RequiresAction(action = Actions.READ)
    fun listManualHoldings(
        @PathVariable brokerId: Long
    ): ResponseEntity<ApiResponse<ManualHoldingListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_LIST.code,
                message = ResponseCode.MANUAL_HOLDING_LIST.message,
                result = manualHoldingService.listManualHoldings(brokerId)
            ), HttpStatus.OK
        )
    }

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun createManualHolding(
        @Valid @RequestBody req: ManualHoldingCreateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_CREATE.code,
                message = ResponseCode.MANUAL_HOLDING_CREATE.message,
                result = manualHoldingService.createManualHolding(req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/{holdingId}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateManualHolding(
        @PathVariable holdingId: Long,
        @Valid @RequestBody req: ManualHoldingUpdateReq
    ): ResponseEntity<ApiResponse<ManualHoldingItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MANUAL_HOLDING_UPDATE.code,
                message = ResponseCode.MANUAL_HOLDING_UPDATE.message,
                result = manualHoldingService.updateManualHolding(holdingId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/{holdingId}")
    @RequiresAction(action = Actions.DELETE)
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

    @PatchMapping("/balance/{memberBrokerId}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateBalance(
        @PathVariable memberBrokerId: Long,
        @RequestBody req: MemberBrokerBalanceUpdateReq
    ): ResponseEntity<ApiResponse<Long>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MEMBER_BROKER_BALANCE_UPDATE.code,
                message = ResponseCode.MEMBER_BROKER_BALANCE_UPDATE.message,
                result = manualHoldingService.updateBalance(memberBrokerId, req.balance)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/reorder")
    @RequiresAction(action = Actions.UPDATE)
    fun reorderManualHoldings(
        @RequestBody req: HoldingReorderReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        manualHoldingService.reorderManualHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.HOLDING_REORDER.code,
                message = ResponseCode.HOLDING_REORDER.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
