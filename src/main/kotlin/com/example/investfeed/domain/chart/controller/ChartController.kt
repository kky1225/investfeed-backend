package com.example.investfeed.domain.chart.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.chart.dto.req.ChartDrawingSaveReq
import com.example.investfeed.domain.chart.dto.req.ChartSettingReq
import com.example.investfeed.domain.chart.dto.res.ChartDrawingRes
import com.example.investfeed.domain.chart.dto.res.ChartSettingRes
import com.example.investfeed.domain.chart.service.ChartService
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.CHART)
@RestController
@RequestMapping("/api/chart")
class ChartController(
    private val chartService: ChartService,
) {

    @GetMapping("settings")
    @RequiresAction(action = Actions.READ)
    fun getSetting(
        @AuthenticationPrincipal user: CustomUserDetails,
    ): ResponseEntity<ApiResponse<ChartSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_SETTING.code,
                message = ResponseCode.CHART_SETTING.message,
                result = chartService.getSetting(user.member.id)
            ), HttpStatus.OK
        )
    }

    @PutMapping("settings")
    @RequiresAction(action = Actions.UPDATE)
    fun saveSetting(
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody req: ChartSettingReq,
    ): ResponseEntity<ApiResponse<ChartSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_SETTING_SAVE.code,
                message = ResponseCode.CHART_SETTING_SAVE.message,
                result = chartService.saveSetting(user.member.id, req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("drawings/{assetType}/{assetCode}")
    @RequiresAction(action = Actions.READ)
    fun getDrawings(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable assetType: AssetType,
        @PathVariable assetCode: String,
    ): ResponseEntity<ApiResponse<ChartDrawingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_DRAWING.code,
                message = ResponseCode.CHART_DRAWING.message,
                result = chartService.getDrawings(user.member.id, assetType, assetCode)
            ), HttpStatus.OK
        )
    }

    @PutMapping("drawings/{assetType}/{assetCode}")
    @RequiresAction(action = Actions.UPDATE)
    fun saveDrawings(
        @AuthenticationPrincipal user: CustomUserDetails,
        @PathVariable assetType: AssetType,
        @PathVariable assetCode: String,
        @RequestBody req: ChartDrawingSaveReq,
    ): ResponseEntity<ApiResponse<ChartDrawingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_DRAWING_SAVE.code,
                message = ResponseCode.CHART_DRAWING_SAVE.message,
                result = chartService.saveDrawings(user.member.id, assetType, assetCode, req)
            ), HttpStatus.OK
        )
    }
}
