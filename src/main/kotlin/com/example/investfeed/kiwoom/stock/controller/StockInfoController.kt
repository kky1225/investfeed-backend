package com.example.investfeed.kiwoom.stock.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoTradeDailyReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoJumpListReq
import com.example.investfeed.kiwoom.stock.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoTradeDailyRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoJumpListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stock.dto.res.StockInfoRes
import com.example.investfeed.kiwoom.stock.service.StockInfoService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/stockinfo")
class StockInfoController(
    private val stockInfoService: StockInfoService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun stockInfoList(
        req: StockInfoListReq
    ): ResponseEntity<ApiResponse<StockInfoListRes?>> {
        log.info { "stockInfoList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO_LIST.code,
                message = ResponseCode.STOCK_INFO_LIST.message,
                result = stockInfoService.stockInfoList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("{stockNm}")
    fun stockInfo(
        @PathVariable("stockNm") stockNm: String
    ): ResponseEntity<ApiResponse<StockInfoRes?>> {
        log.info { "stockInfo $stockNm" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO.code,
                message = ResponseCode.STOCK_INFO.message,
                result = stockInfoService.stockInfo(stockNm = stockNm)
            ), HttpStatus.OK
        )
    }

    @GetMapping("tradeDaily")
    fun stockInfoTradeDaily(
        req: StockInfoTradeDailyReq
    ): ResponseEntity<ApiResponse<StockInfoTradeDailyRes?>> {
        log.info { "stockInfoTradeDaily $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO_TRADE_DAILY.code,
                message = ResponseCode.STOCK_INFO_TRADE_DAILY.message,
                result = stockInfoService.stockInfoTradeDaily(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("jumpList")
    fun stockInfoJumpList(
        req: StockInfoJumpListReq
    ): ResponseEntity<ApiResponse<StockInfoJumpListRes?>> {
        log.info { "stockInfoDailyTrade $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_INFO_JUMP_LIST.code,
                message = ResponseCode.STOCK_INFO_JUMP_LIST.message,
                result = stockInfoService.stockInfoJumpList(req = req)
            ), HttpStatus.OK
        )
    }
}