package com.example.investfeed.domain.realizedpnl.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlSummaryReq
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlDashboardItem
import com.example.investfeed.domain.realizedpnl.dto.res.RealizedPnlSummaryRes
import com.example.investfeed.domain.realizedpnl.service.RealizedPnlSummaryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/realizedpnl")
class RealizedPnlSummaryController(
    private val realizedPnlSummaryService: RealizedPnlSummaryService,
) {

    @PostMapping("summary")
    fun summary(
        @RequestBody req: RealizedPnlSummaryReq
    ): ResponseEntity<ApiResponse<RealizedPnlSummaryRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_SUMMARY.code,
                message = ResponseCode.REALIZED_PNL_SUMMARY.message,
                result = realizedPnlSummaryService.getSummary(req.year)
            ), HttpStatus.OK
        )
    }

    @PostMapping("dashboard")
    fun dashboard(): ResponseEntity<ApiResponse<RealizedPnlDashboardItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REALIZED_PNL_SUMMARY.code,
                message = ResponseCode.REALIZED_PNL_SUMMARY.message,
                result = realizedPnlSummaryService.getDashboardSummary()
            ), HttpStatus.OK
        )
    }
}
