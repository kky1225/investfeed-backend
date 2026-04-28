package com.example.investfeed.domain.sect.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.sect.dto.req.SectListReq
import com.example.investfeed.domain.sect.dto.req.SectListStreamReq
import com.example.investfeed.domain.sect.dto.req.SectStockListReq
import com.example.investfeed.domain.sect.dto.res.SectListRes
import com.example.investfeed.domain.sect.dto.res.SectStockListRes
import com.example.investfeed.domain.sect.service.SectService
import com.example.investfeed.common.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stock/sects")
class SectController(
    private val sectService: SectService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    fun listSects(
        req: SectListReq
    ): ResponseEntity<ApiResponse<SectListRes>> {
        log.info { "listSects : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_LIST.code,
                message = ResponseCode.SECT_LIST.message,
                result = sectService.listSects(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/stream")
    fun streamSects(
        req: SectListStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamSects : $req" }

        sectService.streamSects(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_LIST_STREAM.code,
                message = ResponseCode.SECT_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{indsCd}/stocks")
    fun listStocksBySect(
        @PathVariable indsCd: String,
        req: SectStockListReq
    ): ResponseEntity<ApiResponse<SectStockListRes>> {
        log.info { "listStocksBySect : indsCd=$indsCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_STOCK_LIST.code,
                message = ResponseCode.SECT_STOCK_LIST.message,
                result = sectService.listStocksBySect(indsCd = indsCd, req = req)
            ), HttpStatus.OK
        )
    }
}