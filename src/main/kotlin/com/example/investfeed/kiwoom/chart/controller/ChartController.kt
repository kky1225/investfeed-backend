package com.example.investfeed.kiwoom.chart.controller

import com.example.investfeed.kiwoom.chart.dto.req.ChartDayListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartMinuteListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartMonthListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartTickListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartWeekListReq
import com.example.investfeed.kiwoom.chart.dto.req.ChartYearListReq
import com.example.investfeed.kiwoom.chart.dto.res.ChartDayListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartMinuteListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartMonthListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartTickListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartWeekListRes
import com.example.investfeed.kiwoom.chart.dto.res.ChartYearListRes
import com.example.investfeed.kiwoom.chart.service.ChartService
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/chart")
class ChartController(
    private val chartService: ChartService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("tickList")
    fun chartTickList(
        req: ChartTickListReq
    ): ResponseEntity<ApiResponse<ChartTickListRes?>> {
        log.info { "chartTickList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_TICK_LIST.code,
                message = ResponseCode.CHART_TICK_LIST.message,
                result = chartService.chartTickList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("minuteList")
    fun chartMinuteList(
        req: ChartMinuteListReq
    ): ResponseEntity<ApiResponse<ChartMinuteListRes?>> {
        log.info { "chartMinuteList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_MINUTE_LIST.code,
                message = ResponseCode.CHART_MINUTE_LIST.message,
                result = chartService.chartMinuteList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("dayList")
    fun chartDayList(
        req: ChartDayListReq
    ): ResponseEntity<ApiResponse<ChartDayListRes?>> {
        log.info { "chartDayList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_DAY_LIST.code,
                message = ResponseCode.CHART_DAY_LIST.message,
                result = chartService.chartDayList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("weekList")
    fun chartWeekList(
        req: ChartWeekListReq
    ): ResponseEntity<ApiResponse<ChartWeekListRes?>> {
        log.info { "chartWeekList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_WEEK_LIST.code,
                message = ResponseCode.CHART_WEEK_LIST.message,
                result = chartService.chartWeekList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("monthList")
    fun chartMonthList(
        req: ChartMonthListReq
    ): ResponseEntity<ApiResponse<ChartMonthListRes?>> {
        log.info { "chartMonthList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_MONTH_LIST.code,
                message = ResponseCode.CHART_MONTH_LIST.message,
                result = chartService.chartMonthList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("yearList")
    fun chartYearList(
        req: ChartYearListReq
    ): ResponseEntity<ApiResponse<ChartYearListRes?>> {
        log.info { "chartYearList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CHART_YEAR_LIST.code,
                message = ResponseCode.CHART_YEAR_LIST.message,
                result = chartService.chartYearList(req = req)
            ), HttpStatus.OK
        )
    }
}