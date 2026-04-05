package com.example.investfeed.domain.stock.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockProgramChartReq
import com.example.investfeed.domain.stock.dto.req.StockSearchReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.StockDetailRes
import com.example.investfeed.domain.stock.dto.res.StockProgramChart
import com.example.investfeed.domain.stock.dto.res.StockSearchItem
import com.example.investfeed.domain.stock.service.StockService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/stock")
class StockController(
    private val stockService: StockService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("detail")
    fun stockDetail(
        req: StockDetailReq
    ): ResponseEntity<ApiResponse<StockDetailRes>> {
        log.info { "stockInfo $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL.code,
                message = ResponseCode.STOCK_DETAIL.message,
                result = stockService.stockDetail(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("search")
    fun stockSearch(
        req: StockSearchReq
    ): ResponseEntity<ApiResponse<List<StockSearchItem>>> {
        log.info { "stockSearch : ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_SEARCH.code,
                message = ResponseCode.STOCK_SEARCH.message,
                result = stockService.stockSearch(req.keyword)
            ), HttpStatus.OK
        )
    }

    @GetMapping("program-chart")
    fun stockProgramChart(
        req: StockProgramChartReq
    ): ResponseEntity<ApiResponse<List<StockProgramChart>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_PROGRAM_CHART.code,
                message = ResponseCode.STOCK_PROGRAM_CHART.message,
                result = stockService.stockProgramChart(stkCd = req.stkCd)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    fun stockStream(
       @RequestBody req: StockStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "stockDetailStream $req" }

        stockService.stockStream(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL_WS.code,
                message = ResponseCode.STOCK_DETAIL_WS.message,
                result = null
            ), HttpStatus.OK
        )
    }
}