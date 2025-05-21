package com.example.investfeed.kiwoom.etf.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.etf.dto.req.EtfInfoReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfPriceListReq
import com.example.investfeed.kiwoom.etf.dto.req.EtfTradeDailyListReq
import com.example.investfeed.kiwoom.etf.dto.res.EtfInfoRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfPriceListRes
import com.example.investfeed.kiwoom.etf.dto.res.EtfTradeDailyListRes
import com.example.investfeed.kiwoom.etf.service.EtfService
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/etf")
class EtfController(
    private val etfService: EtfService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("priceList")
    fun etfPriceList(
        req: EtfPriceListReq
    ): ResponseEntity<ApiResponse<EtfPriceListRes?>> {
        log.info { "etfPriceList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ETF_PRICE_LIST.code,
                message = ResponseCode.ETF_PRICE_LIST.message,
                result = etfService.etfPriceList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("info")
    fun etfInfo(
        req: EtfInfoReq
    ): ResponseEntity<ApiResponse<EtfInfoRes?>> {
        log.info { "etfInfo $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ETF_INFO.code,
                message = ResponseCode.ETF_INFO.message,
                result = etfService.etfInfo(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("tradeDailyList")
    fun etfTradeDailyList(
        req: EtfTradeDailyListReq
    ): ResponseEntity<ApiResponse<EtfTradeDailyListRes?>> {
        log.info { "etfTradeDailyList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ETF_TRADE_DAILY_LIST.code,
                message = ResponseCode.ETF_TRADE_DAILY_LIST.message,
                result = etfService.etfTradeDailyList(req = req)
            ), HttpStatus.OK
        )
    }
}