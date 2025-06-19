package com.example.investfeed.kiwoom.dashboard.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.dashboard.dto.res.DashboardRes
import com.example.investfeed.kiwoom.dashboard.service.DashboardService
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectIndexListReq
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
class DashboardController(
    private val dashboardService: DashboardService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("dashboard")
    fun dashboard(
        @RequestBody req: SectIndexListReq
    ): ResponseEntity<ApiResponse<DashboardRes?>> {
        log.info { "dashboard $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.DASHBOARD.code,
                message = ResponseCode.DASHBOARD.message,
                dashboardService.dashboard(req = req)
            ), HttpStatus.OK
        )
    }
}