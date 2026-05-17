package com.example.investfeed.domain.recommend.admin.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.recommend.admin.dto.res.AdminBackfillStatusRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminBacktestMetricsRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminMarketSnapshotRes
import com.example.investfeed.domain.recommend.admin.dto.res.AdminRecommendPickRes
import com.example.investfeed.domain.recommend.admin.service.AdminRecommendMonitoringService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RequiresAction(permission = Permissions.ADMIN_RECOMMEND_MONITORING)
@RestController
@RequestMapping("/api/admin/recommend/monitoring")
class AdminRecommendMonitoringController(
    private val adminRecommendMonitoringService: AdminRecommendMonitoringService,
) {
    @GetMapping("/picks")
    @RequiresAction(action = Actions.READ)
    fun listPicks(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
    ): ResponseEntity<ApiResponse<List<AdminRecommendPickRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_RECOMMEND_PICKS.code,
                message = ResponseCode.ADMIN_RECOMMEND_PICKS.message,
                result = adminRecommendMonitoringService.listPicks(date),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/snapshots")
    @RequiresAction(action = Actions.READ)
    fun listSnapshots(
        @RequestParam(defaultValue = "30") days: Int,
    ): ResponseEntity<ApiResponse<List<AdminMarketSnapshotRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_RECOMMEND_SNAPSHOTS.code,
                message = ResponseCode.ADMIN_RECOMMEND_SNAPSHOTS.message,
                result = adminRecommendMonitoringService.listMarketSnapshots(days),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/backfill-status")
    @RequiresAction(action = Actions.READ)
    fun listBackfillStatus(
        @RequestParam(defaultValue = "25") days: Int,
    ): ResponseEntity<ApiResponse<List<AdminBackfillStatusRes>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_RECOMMEND_BACKFILL_STATUS.code,
                message = ResponseCode.ADMIN_RECOMMEND_BACKFILL_STATUS.message,
                result = adminRecommendMonitoringService.listBackfillStatus(days),
            ), HttpStatus.OK
        )
    }

    @GetMapping("/metrics")
    @RequiresAction(action = Actions.READ)
    fun computeMetrics(
        @RequestParam(defaultValue = "30") periodDays: Int,
    ): ResponseEntity<ApiResponse<AdminBacktestMetricsRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ADMIN_RECOMMEND_METRICS.code,
                message = ResponseCode.ADMIN_RECOMMEND_METRICS.message,
                result = adminRecommendMonitoringService.computeMetrics(periodDays),
            ), HttpStatus.OK
        )
    }
}
