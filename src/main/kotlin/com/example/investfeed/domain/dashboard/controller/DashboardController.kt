package com.example.investfeed.domain.dashboard.controller

import com.example.investfeed.domain.dashboard.dto.res.DashboardRes
import com.example.investfeed.domain.dashboard.service.DashboardService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
class DashboardController(
    private val dashboardService: DashboardService
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("dashboard")
    fun dashboard(): ResponseEntity<ApiResponse<DashboardRes?>> {
        log.info { "dashboard" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.DASHBOARD.code,
                message = ResponseCode.DASHBOARD.message,
                dashboardService.dashboard()
            ), HttpStatus.OK
        )
    }
}