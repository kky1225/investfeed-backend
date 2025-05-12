package com.example.investfeed.kiwoom.stockinfo.controller

import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.stockinfo.dto.req.StockInfoListReq
import com.example.investfeed.kiwoom.stockinfo.dto.res.StockInfoListRes
import com.example.investfeed.kiwoom.stockinfo.service.StockInfoService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
                code = "0000",
                message = "success",
                result = stockInfoService.stockInfoList(req)
            ), HttpStatus.OK
        )
    }
}