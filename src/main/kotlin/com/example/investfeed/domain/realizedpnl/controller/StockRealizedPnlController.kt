package com.example.investfeed.domain.realizedpnl.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.realizedpnl.dto.req.*
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlListRes
import com.example.investfeed.domain.realizedpnl.service.ManualRealizedPnlService
import com.example.investfeed.domain.realizedpnl.service.StockRealizedPnlService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.STOCK_REALIZED_PNL)
@RestController
@RequestMapping("/api/stock/realized-pnl")
class StockRealizedPnlController(
    private val stockRealizedPnlService: StockRealizedPnlService,
    private val manualRealizedPnlService: ManualRealizedPnlService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listStockRealizedPnls(
        @ModelAttribute req: RealizedPnlListReq
    ): ResponseEntity<ApiResponse<RealizedPnlListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_LIST.code,
                message = ResponseCode.REALIZED_PNL_LIST.message,
                result = manualRealizedPnlService.listStockRealizedPnls(req.year, req.month)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/sync")
    @RequiresAction(action = Actions.CREATE)
    fun syncStockRealizedPnls(
        @RequestBody req: RealizedPnlSyncReq
    ): ResponseEntity<ApiResponse<RealizedPnlListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_SYNC.code,
                message = ResponseCode.REALIZED_PNL_SYNC.message,
                result = stockRealizedPnlService.syncStockRealizedPnls(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/manual")
    @RequiresAction(action = Actions.CREATE)
    fun createManualStockPnl(
        @Valid @RequestBody req: ManualRealizedPnlCreateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_CREATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_CREATE.message,
                result = manualRealizedPnlService.createManualStockPnl(req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/manual/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateManualStockPnl(
        @PathVariable id: Long,
        @RequestBody req: ManualRealizedPnlUpdateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.message,
                result = manualRealizedPnlService.updateManualStockPnl(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/manual/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteManualStockPnl(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        manualRealizedPnlService.deleteManualStockPnl(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_DELETE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
