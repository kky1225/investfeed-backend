package com.example.investfeed.kiwoom.investor.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeDailyReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeOrganizeReq
import com.example.investfeed.kiwoom.investor.dto.req.InvestorTradeRankListReq
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeDailyRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeOrganizeRes
import com.example.investfeed.kiwoom.investor.dto.res.InvestorTradeRankListRes
import com.example.investfeed.kiwoom.investor.client.InvestorClient
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/investor")
class InvestorController(
    private val investorClient: InvestorClient
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("trade/daily")
    fun investorTradeDaily(
        req: InvestorTradeDailyReq
    ): ResponseEntity<ApiResponse<InvestorTradeDailyRes?>> {
        log.info { "investorTradeDaily $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INVESTOR_TRADE_DAILY.code,
                message = ResponseCode.INVESTOR_TRADE_DAILY.message,
                result = investorClient.investorTradeDaily(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("trade/organize")
    fun investorTradeOrganize(
        req: InvestorTradeOrganizeReq
    ): ResponseEntity<ApiResponse<InvestorTradeOrganizeRes?>> {
        log.info { "investorTradeOrganize $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INVESTOR_TRADE_ORGANIZE.code,
                message = ResponseCode.INVESTOR_TRADE_ORGANIZE.message,
                result = investorClient.investorTradeOrganize(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("trade/rankList")
    fun investorTradeRankList(
        req: InvestorTradeRankListReq
    ): ResponseEntity<ApiResponse<InvestorTradeRankListRes?>> {
        log.info { "investorTradeRankList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.INVESTOR_TRADE_RANK_LIST.code,
                message = ResponseCode.INVESTOR_TRADE_RANK_LIST.message,
                result = investorClient.investorTradeRankList(req = req)
            ), HttpStatus.OK
        )
    }
}