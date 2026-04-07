package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.res.AssetDashboardRes
import com.example.investfeed.domain.holding.service.AssetDashboardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/asset")
class AssetDashboardController(
    private val assetDashboardService: AssetDashboardService
) {

    @GetMapping("dashboard")
    fun dashboard(): ResponseEntity<ApiResponse<AssetDashboardRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ASSET_DASHBOARD.code,
                message = ResponseCode.ASSET_DASHBOARD.message,
                result = assetDashboardService.dashboard()
            ), HttpStatus.OK
        )
    }
}
