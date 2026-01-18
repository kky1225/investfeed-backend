package com.example.investfeed.domain.sect.controller

import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.req.SectListStreamReq
import com.example.investfeed.domain.sect.dto.req.SectStockListReq
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.domain.sect.dto.res.SectStockListRes
import com.example.investfeed.domain.sect.service.SectService
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sect")
class SectController(
    private val sectService: SectService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun sectList(
        req: SectListReq
    ): ResponseEntity<ApiResponse<SectListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_LIST.code,
                message = ResponseCode.SECT_LIST.message,
                result = sectService.sectList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("list/stream")
    fun sectListStream(
        req: SectListStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        sectService.sectListStream(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_LIST_STREAM.code,
                message = ResponseCode.SECT_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("stockList")
    fun sectStockList(
        req: SectStockListReq
    ): ResponseEntity<ApiResponse<SectStockListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_STOCK_LIST.code,
                message = ResponseCode.SECT_STOCK_LIST.message,
                result = sectService.sectStockList(req = req)
            ), HttpStatus.OK
        )
    }
}