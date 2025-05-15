package com.example.investfeed.kiwoom.sect.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.sect.dto.req.SectCodeListReq
import com.example.investfeed.kiwoom.sect.dto.req.SectIndexListReq
import com.example.investfeed.kiwoom.sect.dto.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceNowReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceReq
import com.example.investfeed.kiwoom.sect.dto.res.SectCodeListRes
import com.example.investfeed.kiwoom.sect.dto.res.SectIndexListRes
import com.example.investfeed.kiwoom.sect.dto.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.SectPriceNowRes
import com.example.investfeed.kiwoom.sect.dto.res.SectPriceRes
import com.example.investfeed.kiwoom.sect.service.SectService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/sect")
class SectController(
    private val sectService: SectService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("investor")
    fun sectInvestor(
        req: SectInvestorReq
    ): ResponseEntity<ApiResponse<SectInvestorRes?>> {
        log.info { "sectInvestor $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_INVESTOR.code,
                message = ResponseCode.SECT_INVESTOR.message,
                result = sectService.sectInvestor(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("priceNow")
    fun sectPriceNow(
        req: SectPriceNowReq
    ): ResponseEntity<ApiResponse<SectPriceNowRes?>> {
        log.info { "sectNowPrice $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_PRICE_NOW.code,
                message = ResponseCode.SECT_PRICE_NOW.message,
                result = sectService.sectPriceNow(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("price")
    fun sectPrice(
        req: SectPriceReq
    ): ResponseEntity<ApiResponse<SectPriceRes?>> {
        log.info { "sectPrice $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_PRICE.code,
                message = ResponseCode.SECT_PRICE.message,
                result = sectService.sectPrice(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("codeList")
    fun sectCodeList(
        req: SectCodeListReq
    ): ResponseEntity<ApiResponse<SectCodeListRes?>> {
        log.info { "sectCodeList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_CODE_LIST.code,
                message = ResponseCode.SECT_CODE_LIST.message,
                result = sectService.sectCodeList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("indexList")
    fun sectIndexList(
        req: SectIndexListReq
    ): ResponseEntity<ApiResponse<SectIndexListRes?>> {
        log.info { "sectIndex $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.SECT_INDEX_LIST.code,
                message = ResponseCode.SECT_INDEX_LIST.message,
                result = sectService.sectIndexList(req = req)
            ), HttpStatus.OK
        )
    }
}