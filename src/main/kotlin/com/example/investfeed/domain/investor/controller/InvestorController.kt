package com.example.investfeed.domain.investor.controller

import com.example.investfeed.domain.investor.dto.req.InvestorListReq
import com.example.investfeed.domain.investor.dto.req.InvestorStreamReq
import com.example.investfeed.domain.investor.dto.res.InvestorListRes
import com.example.investfeed.domain.investor.service.InvestorService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RequiresAction(permission = Permissions.STOCK_INVESTOR)
@RestController
@RequestMapping("/api/stock/investors")
class InvestorController(
    private val investorService: InvestorService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listInvestors(
        req: InvestorListReq
    ): ResponseEntity<ApiResponse<InvestorListRes?>> {
        log.info { "investorList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INVESTOR_LIST.code,
                message = ResponseCode.INVESTOR_LIST.message,
                result = investorService.listInvestors(req = req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamInvestors(
        @RequestBody req: InvestorStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "stockStream $req" }

        investorService.streamInvestors(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL_WS.code,
                message = ResponseCode.STOCK_DETAIL_WS.message,
                result = null
            ), HttpStatus.OK
        )
    }
}