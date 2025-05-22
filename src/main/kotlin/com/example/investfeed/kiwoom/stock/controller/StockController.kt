package com.example.investfeed.kiwoom.stock.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.stock.dto.req.StockTradeDailyListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockJumpListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoReq
import com.example.investfeed.kiwoom.stock.dto.req.StockSinglePriceListReq
import com.example.investfeed.kiwoom.stock.dto.res.StockTradeDailyListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockJumpListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoRes
import com.example.investfeed.kiwoom.stock.dto.res.StockSinglePriceListRes
import com.example.investfeed.kiwoom.stock.service.StockService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/stock")
class StockController(
    private val stockService: StockService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("infoList")
    fun stockInfoList(
        req: StockInfoListReq
    ): ResponseEntity<ApiResponse<StockInfoListRes?>> {
        log.info { "stockInfoList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO_LIST.code,
                message = ResponseCode.STOCK_INFO_LIST.message,
                result = stockService.stockInfoList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("info")
    fun stockInfo(
        req: StockInfoReq
    ): ResponseEntity<ApiResponse<StockInfoRes?>> {
        log.info { "stockInfo $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO.code,
                message = ResponseCode.STOCK_INFO.message,
                result = stockService.stockInfo(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("tradeDailyList")
    fun stockTradeDailyList(
        req: StockTradeDailyListReq
    ): ResponseEntity<ApiResponse<StockTradeDailyListRes?>> {
        log.info { "stockTradeDailyList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_TRADE_DAILY_LIST.code,
                message = ResponseCode.STOCK_TRADE_DAILY_LIST.message,
                result = stockService.stockTradeDailyList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("jumpList")
    fun stockJumpList(
        req: StockJumpListReq
    ): ResponseEntity<ApiResponse<StockJumpListRes?>> {
        log.info { "stockJumpList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_JUMP_LIST.code,
                message = ResponseCode.STOCK_JUMP_LIST.message,
                result = stockService.stockJumpList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("singlePriceList")
    fun stockSinglePriceList(
        req: StockSinglePriceListReq
    ): ResponseEntity<ApiResponse<StockSinglePriceListRes?>> {
        log.info { "stockSinglePriceList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_SINGLE_PRICE_LIST.code,
                message = ResponseCode.STOCK_SINGLE_PRICE_LIST.message,
                result = stockService.stockSinglePriceList(req = req)
            ), HttpStatus.OK
        )
    }
}