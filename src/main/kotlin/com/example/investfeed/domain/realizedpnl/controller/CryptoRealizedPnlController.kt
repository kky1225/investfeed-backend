package com.example.investfeed.domain.realizedpnl.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.realizedpnl.dto.req.ManualRealizedPnlCreateReq
import com.example.investfeed.domain.realizedpnl.dto.req.ManualRealizedPnlUpdateReq
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlListReq
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlListRes
import com.example.investfeed.domain.realizedpnl.service.CryptoRealizedPnlService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/crypto/realizedpnl")
class CryptoRealizedPnlController(
    private val cryptoRealizedPnlService: CryptoRealizedPnlService,
) {

    @PostMapping("list")
    fun list(
        @RequestBody req: RealizedPnlListReq
    ): ResponseEntity<ApiResponse<RealizedPnlListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_LIST.code,
                message = ResponseCode.REALIZED_PNL_LIST.message,
                result = cryptoRealizedPnlService.list(req.year, req.month)
            ), HttpStatus.OK
        )
    }

    @PostMapping("manual/create")
    fun createManual(
        @Valid @RequestBody req: ManualRealizedPnlCreateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_CREATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_CREATE.message,
                result = cryptoRealizedPnlService.create(req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("manual/{id}")
    fun updateManual(
        @PathVariable id: Long,
        @RequestBody req: ManualRealizedPnlUpdateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.message,
                result = cryptoRealizedPnlService.update(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("manual/{id}")
    fun deleteManual(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoRealizedPnlService.delete(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_DELETE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
