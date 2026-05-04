package com.example.investfeed.domain.rebalancing.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.rebalancing.dto.req.RebalancingSettingReq
import com.example.investfeed.domain.rebalancing.dto.res.RebalancingSettingRes
import com.example.investfeed.domain.rebalancing.dto.res.RebalancingStatusRes
import com.example.investfeed.domain.rebalancing.service.RebalancingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.REBALANCING)
@RestController
@RequestMapping("/api/rebalancing")
class RebalancingController(
    private val rebalancingService: RebalancingService
) {

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
    fun saveRebalancing(
        @Valid @RequestBody req: RebalancingSettingReq
    ): ResponseEntity<ApiResponse<RebalancingSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_SETTING_SAVE.code,
                message = ResponseCode.REBALANCING_SETTING_SAVE.message,
                result = rebalancingService.saveRebalancing(req)
            ), HttpStatus.OK
        )
    }

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun getRebalancing(): ResponseEntity<ApiResponse<RebalancingStatusRes?>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_STATUS.code,
                message = ResponseCode.REBALANCING_STATUS.message,
                result = rebalancingService.getRebalancing()
            ), HttpStatus.OK
        )
    }

    @DeleteMapping
    @RequiresAction(action = Actions.DELETE)
    fun deleteRebalancing(): ResponseEntity<ApiResponse<Nothing?>> {
        rebalancingService.deleteRebalancing()
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.REBALANCING_SETTING_DELETE.code,
                message = ResponseCode.REBALANCING_SETTING_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
