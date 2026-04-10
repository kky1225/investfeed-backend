package com.example.investfeed.domain.rebalancing.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.rebalancing.dto.req.RebalancingSettingReq
import com.example.investfeed.domain.rebalancing.dto.res.RebalancingSettingRes
import com.example.investfeed.domain.rebalancing.dto.res.RebalancingStatusRes
import com.example.investfeed.domain.rebalancing.service.RebalancingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/rebalancing")
class RebalancingController(
    private val rebalancingService: RebalancingService
) {

    @PostMapping("setting")
    fun saveSetting(
        @RequestBody req: RebalancingSettingReq
    ): ResponseEntity<ApiResponse<RebalancingSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_SETTING_SAVE.code,
                message = ResponseCode.REBALANCING_SETTING_SAVE.message,
                result = rebalancingService.saveSetting(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("status")
    fun status(): ResponseEntity<ApiResponse<RebalancingStatusRes?>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_STATUS.code,
                message = ResponseCode.REBALANCING_STATUS.message,
                result = rebalancingService.getStatus()
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("setting")
    fun deleteSetting(): ResponseEntity<ApiResponse<Nothing?>> {
        rebalancingService.deleteSetting()
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_SETTING_DELETE.code,
                message = ResponseCode.REBALANCING_SETTING_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
