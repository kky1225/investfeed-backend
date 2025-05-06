package com.example.investfeed.kiwoom.sect.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.sect.dto.req.SectInvestorReq
import com.example.investfeed.kiwoom.sect.dto.req.SectPriceReq
import com.example.investfeed.kiwoom.sect.dto.res.SectInvestorRes
import com.example.investfeed.kiwoom.sect.dto.res.SectPriceRes
import com.example.investfeed.kiwoom.sect.service.SectService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("api/sect")
@RestController
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
                result = sectService.sectInvestor(req)
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
                result = sectService.sectPrice(req)
            ), HttpStatus.OK
        )
    }
}