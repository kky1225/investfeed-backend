package com.example.investfeed.domain.dashboard.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.domain.dashboard.dto.req.DashboardStreamReq
import com.example.investfeed.domain.dashboard.dto.res.DashboardRes
import com.example.investfeed.domain.dashboard.service.DashboardService
import com.example.investfeed.kiwoom.stock.client.StockSocketClient
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStream
import com.example.investfeed.kiwoom.realtime.dto.SectIndexListStreamReq
import com.example.investfeed.kiwoom.realtime.client.RealTimeClient
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStream
import com.example.investfeed.kiwoom.stock.dto.req.KiwoomStockStreamReq
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
    private val dashboardService: DashboardService,
    private val stockSocketClient: StockSocketClient,
    private val realTimeClient: RealTimeClient
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

        realTimeClient.sectIndexListStream(
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

        stockSocketClient.stockListStream(
            req = KiwoomStockStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomStockStream(
                        item = req.items,
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