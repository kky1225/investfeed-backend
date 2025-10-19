package com.example.investfeed.kiwoom.rank.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.rank.dto.req.RankTradeDailyVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.req.RankTradeVolumeListReq
import com.example.investfeed.kiwoom.rank.dto.res.RankTradeDailyVolumeListRes
import com.example.investfeed.kiwoom.rank.dto.res.RankTradeVolumeListRes
import com.example.investfeed.kiwoom.rank.client.RankClient
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/rank")
class RankController(
  private val rankClient: RankClient
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("tradeVolumeList")
    fun rankTradeVolumeList(
        req: RankTradeVolumeListReq
    ): ResponseEntity<ApiResponse<RankTradeVolumeListRes?>> {
        log.info { "rankTradeVolumeList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RANK_TRADE_VOLUME_LIST.code,
                message = ResponseCode.RANK_TRADE_VOLUME_LIST.message,
                result = rankClient.rankTradeVolumeList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("tradeDailyVolumeList")
    fun rankTradeDailyVolumeList(
        req: RankTradeDailyVolumeListReq
    ): ResponseEntity<ApiResponse<RankTradeDailyVolumeListRes?>> {
        log.info { "rankTradeDailyVolumeList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RANK_TRADE_DAILY_VOLUME_LIST.code,
                message = ResponseCode.RANK_TRADE_DAILY_VOLUME_LIST.message,
                result = rankClient.rankTradeDailyVolumeList(req = req)
            ), HttpStatus.OK
        )
    }
}