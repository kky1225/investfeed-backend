package com.example.investfeed.domain.investor.controller

import com.example.investfeed.domain.investor.service.InvestorService
import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/investor")
class InvestorController(
    private val investorService: InvestorService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("/list")
    fun investorList(
        req: InvestorListReq
    ): ResponseEntity<ApiResponse<InvestorListRes?>> {
        log.info { "investorList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INVESTOR_LIST.code,
                message = ResponseCode.INVESTOR_LIST.message,
                result = investorService.investorList(req = req)
            ), HttpStatus.OK
        )
    }
}