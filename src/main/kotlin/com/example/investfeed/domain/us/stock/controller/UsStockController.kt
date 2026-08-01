package com.example.investfeed.domain.us.stock.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.us.stock.dto.req.UsStockDetailReq
import com.example.investfeed.domain.us.stock.dto.req.UsStockDetailStreamReq
import com.example.investfeed.domain.us.stock.dto.req.UsStockSearchReq
import com.example.investfeed.domain.us.stock.dto.res.UsStockDetailRes
import com.example.investfeed.domain.us.stock.dto.res.UsStockSearchItem
import com.example.investfeed.domain.us.stock.service.UsStockInfoService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.US_STOCK_INFO)
@RestController
@RequestMapping("/api/us-stock/stocks")
class UsStockController(
    private val usStockInfoService: UsStockInfoService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun searchUsStocks(
        req: UsStockSearchReq
    ): ResponseEntity<ApiResponse<List<UsStockSearchItem>>> {
        log.info { "searchUsStocks: ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_STOCK_SEARCH.code,
                message = ResponseCode.US_STOCK_SEARCH.message,
                result = usStockInfoService.searchUsStocks(req.keyword)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getUsStock(
        @PathVariable stkCd: String,
        req: UsStockDetailReq
    ): ResponseEntity<ApiResponse<UsStockDetailRes>> {
        log.info { "getUsStock: stkCd=$stkCd, stexTp=${req.stexTp}, chartType=${req.chartType}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_STOCK_DETAIL.code,
                message = ResponseCode.US_STOCK_DETAIL.message,
                result = usStockInfoService.getUsStockDetail(stkCd, req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamUsStock(
        @RequestBody req: UsStockDetailStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        usStockInfoService.streamUsStock(stkCd = req.stkCd, stexTp = req.stexTp)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_STOCK_DETAIL_WS.code,
                message = ResponseCode.US_STOCK_DETAIL_WS.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
