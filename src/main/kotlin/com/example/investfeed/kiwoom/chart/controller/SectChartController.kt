package com.example.investfeed.kiwoom.chart.controller

import com.example.investfeed.kiwoom.chart.dto.sect.req.SectChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.sect.res.SectChartMinuteListRes
import com.example.investfeed.kiwoom.chart.service.SectChartService
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/sect/chart")
class SectChartController(
    private val sectChartService: SectChartService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("minuteList")
    fun sectChartMinuteList(
        req: SectChartMinuteListReq
    ): ResponseEntity<ApiResponse<SectChartMinuteListRes?>> {
        log.info { "sectChartMinuteList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_CHART_MINUTE_LIST.code,
                message = ResponseCode.SECT_CHART_MINUTE_LIST.message,
                result = sectChartService.sectChartMinuteList(req = req)
            ), HttpStatus.OK
        )
    }
}