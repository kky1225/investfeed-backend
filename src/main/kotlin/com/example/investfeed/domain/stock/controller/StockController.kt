package com.example.investfeed.domain.stock.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockSearchReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.StockChartRes
import com.example.investfeed.domain.stock.dto.res.StockDetailRes
import com.example.investfeed.domain.stock.dto.res.StockProgramChart
import com.example.investfeed.domain.stock.dto.res.StockSearchItem
import com.example.investfeed.domain.stock.service.StockService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RequiresAction(permission = Permissions.STOCK_DETAIL)
@RestController
@RequestMapping("/api/stocks")
class StockController(
    private val stockService: StockService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getStock(
        @PathVariable stkCd: String,
        req: StockDetailReq
    ): ResponseEntity<ApiResponse<StockDetailRes>> {
        log.info { "getStock: stkCd=$stkCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL.code,
                message = ResponseCode.STOCK_DETAIL.message,
                result = stockService.getStock(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{stkCd}/chart")
    @RequiresAction(action = Actions.READ)
    fun getStockChart(
        @PathVariable stkCd: String,
        req: StockDetailReq
    ): ResponseEntity<ApiResponse<StockChartRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL.code,
                message = ResponseCode.STOCK_DETAIL.message,
                result = stockService.getStockChart(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun searchStocks(
        req: StockSearchReq
    ): ResponseEntity<ApiResponse<List<StockSearchItem>>> {
        log.info { "searchStocks: ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_SEARCH.code,
                message = ResponseCode.STOCK_SEARCH.message,
                result = stockService.searchStocks(req.keyword)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{stkCd}/program-chart")
    @RequiresAction(action = Actions.READ)
    fun getStockProgramChart(
        @PathVariable stkCd: String
    ): ResponseEntity<ApiResponse<List<StockProgramChart>>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_PROGRAM_CHART.code,
                message = ResponseCode.STOCK_PROGRAM_CHART.message,
                result = stockService.getStockProgramChart(stkCd = stkCd)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamStocks(
        @RequestBody req: StockStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamStocks: $req" }

        stockService.streamStocks(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL_WS.code,
                message = ResponseCode.STOCK_DETAIL_WS.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
