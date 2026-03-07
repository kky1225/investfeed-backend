package com.example.investfeed.domain.stock.controller

import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockListReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.StockDetailRes
import com.example.investfeed.domain.stock.dto.res.StockListRes
import com.example.investfeed.domain.stock.service.StockService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
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

    @GetMapping("list")
    fun stockList(
        req: StockListReq
    ): ResponseEntity<ApiResponse<StockListRes>> {
        log.info { "stockList : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_LIST.code,
                message = ResponseCode.STOCK_LIST.message,
                result = stockService.stockList(req)
            ), HttpStatus.OK
        )
    }

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

//
//    @GetMapping("tradeDailyList")
//    fun stockTradeDailyList(
//        req: StockTradeDailyListReq
//    ): ResponseEntity<ApiResponse<StockTradeDailyListRes?>> {
//        log.info { "stockTradeDailyList $req" }
//
//        return ResponseEntity(
//            ApiResponse(
//                code = ResponseCode.STOCK_TRADE_DAILY_LIST.code,
//                message = ResponseCode.STOCK_TRADE_DAILY_LIST.message,
//                result = stockClient.stockTradeDailyList(req = req)
//            ), HttpStatus.OK
//        )
//    }
//
//    @GetMapping("jumpList")
//    fun stockJumpList(
//        req: StockJumpListReq
//    ): ResponseEntity<ApiResponse<StockJumpListRes?>> {
//        log.info { "stockJumpList $req" }
//
//        return ResponseEntity(
//            ApiResponse(
//                code = ResponseCode.STOCK_JUMP_LIST.code,
//                message = ResponseCode.STOCK_JUMP_LIST.message,
//                result = stockClient.stockJumpList(req = req)
//            ), HttpStatus.OK
//        )
//    }
//
//    @GetMapping("singlePriceList")
//    fun stockSinglePriceList(
//        req: StockSinglePriceListReq
//    ): ResponseEntity<ApiResponse<StockSinglePriceListRes?>> {
//        log.info { "stockSinglePriceList $req" }
//
//        return ResponseEntity(
//            ApiResponse(
//                code = ResponseCode.STOCK_SINGLE_PRICE_LIST.code,
//                message = ResponseCode.STOCK_SINGLE_PRICE_LIST.message,
//                result = stockClient.stockSinglePriceList(req = req)
//            ), HttpStatus.OK
//        )
//    }
//
//    @GetMapping("newPriceList")
//    fun stockNewPriceList(
//        req: StockNewPriceListReq
//    ): ResponseEntity<ApiResponse<StockNewPriceListRes?>> {
//        log.info { "stockNewPriceList $req" }
//
//        return ResponseEntity(
//            ApiResponse(
//                code = ResponseCode.STOCK_NEW_PRICE_LIST.code,
//                message = ResponseCode.STOCK_NEW_PRICE_LIST.message,
//                result = stockClient.stockNewPriceList(req = req)
//            ), HttpStatus.OK
//        )
//    }
}