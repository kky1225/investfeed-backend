package com.example.investfeed.domain.rank.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.rank.dto.req.RankListReq
import com.example.investfeed.domain.rank.dto.res.RankListRes
import com.example.investfeed.domain.rank.service.RankService
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.service.StockService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.STOCK_RANK)
@RestController
@RequestMapping("/api/stock/ranks")
class RankController(
    private val rankService: RankService,
    private val stockService: StockService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listRanks(
        req: RankListReq
    ): ResponseEntity<ApiResponse<RankListRes>> {
        log.info { "listRanks : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RANK_LIST.code,
                message = ResponseCode.RANK_LIST.message,
                result = rankService.listRanks(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamRanks(
        @RequestBody req: StockStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamRanks: $req" }

        stockService.streamStocks(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RANK_STREAM.code,
                message = ResponseCode.RANK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
