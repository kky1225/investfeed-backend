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
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.CRYPTO_REALIZED_PNL)
@RestController
@RequestMapping("/api/crypto/realized-pnl")
class CryptoRealizedPnlController(
    private val cryptoRealizedPnlService: CryptoRealizedPnlService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listCryptoRealizedPnls(
        @ModelAttribute req: RealizedPnlListReq
    ): ResponseEntity<ApiResponse<RealizedPnlListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_LIST.code,
                message = ResponseCode.REALIZED_PNL_LIST.message,
                result = cryptoRealizedPnlService.listCryptoRealizedPnls(req.year, req.month)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/manual")
    @RequiresAction(action = Actions.CREATE)
    fun createManualCryptoPnl(
        @Valid @RequestBody req: ManualRealizedPnlCreateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_CREATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_CREATE.message,
                result = cryptoRealizedPnlService.createManualCryptoPnl(req)
            ), HttpStatus.OK
        )
    }

    @PatchMapping("/manual/{id}")
    @RequiresAction(action = Actions.UPDATE)
    fun updateManualCryptoPnl(
        @PathVariable id: Long,
        @RequestBody req: ManualRealizedPnlUpdateReq
    ): ResponseEntity<ApiResponse<RealizedPnlItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_UPDATE.message,
                result = cryptoRealizedPnlService.updateManualCryptoPnl(id, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/manual/{id}")
    @RequiresAction(action = Actions.DELETE)
    fun deleteManualCryptoPnl(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoRealizedPnlService.deleteManualCryptoPnl(id)
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_MANUAL_DELETE.code,
                message = ResponseCode.REALIZED_PNL_MANUAL_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
