package com.example.investfeed.domain.dashboard.controller

import com.example.investfeed.domain.dashboard.dto.res.DashboardRes
import com.example.investfeed.domain.dashboard.service.DashboardService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.kiwoom.realtime.client.RealTimeClient
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStream
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.STOCK_DASHBOARD)
@RestController
@RequestMapping("/api/stock")
class DashboardController(
    private val dashboardService: DashboardService,
    private val realTimeClient: RealTimeClient,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("/dashboard")
    @RequiresAction(action = Actions.READ)
    fun getStockDashboard(): ResponseEntity<ApiResponse<DashboardRes?>> {
        log.info { "dashboard" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.DASHBOARD.code,
                message = ResponseCode.DASHBOARD.message,
                dashboardService.getStockDashboard()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/dashboard/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamDashboard(): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamDashboard" }

        realTimeClient.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = listOf("001", "101", "201", "150"),
                        type = listOf("0J")
                    )
                )
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.DASHBOARD_STREAM.code,
                message = ResponseCode.DASHBOARD_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
