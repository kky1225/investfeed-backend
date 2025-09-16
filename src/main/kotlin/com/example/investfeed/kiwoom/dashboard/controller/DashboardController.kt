package com.example.investfeed.kiwoom.dashboard.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.dashboard.dto.req.DashboardStreamReq
import com.example.investfeed.kiwoom.dashboard.dto.res.DashboardRes
import com.example.investfeed.kiwoom.dashboard.service.DashboardService
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.sect.dto.rest.req.SectIndexListReq
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStream
import com.example.investfeed.kiwoom.sect.dto.socket.req.SectIndexListStreamReq
import com.example.investfeed.kiwoom.sect.service.SectSocketService
import com.example.investfeed.kiwoom.stock.dto.req.StockListStream
import com.example.investfeed.kiwoom.stock.dto.req.StockListStreamReq
import com.example.investfeed.kiwoom.stock.service.StockSocketService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api")
class DashboardController(
    private val dashboardService: DashboardService,
    private val stockSocketService: StockSocketService,
    private val sectSocketService: SectSocketService
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

    @PostMapping("dashboard/stream")
    fun dashboardStream(
        @RequestBody req: DashboardStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "dashboardStream: $req" }

        sectSocketService.sectIndexListStream(
            req = SectIndexListStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    SectIndexListStream(
                        item = listOf("001", "101", "201"),
                        type = listOf("0J")
                    )
                )
            )
        )

        stockSocketService.stockListStream(
            req = StockListStreamReq(
                trnm = "REG",
                grp_no = "0002",
                refresh = "0",
                data = listOf(
                    StockListStream(
                        item = req.items ?: emptyList(),
                        type = listOf("0A")
                    )
                )
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.DASHBOARD_WS.code,
                message = ResponseCode.DASHBOARD_WS.message,
                result = null
            ), HttpStatus.OK
        )
    }
}