package com.example.investfeed.domain.us.rank.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.us.rank.dto.req.UsRankListReq
import com.example.investfeed.domain.us.rank.dto.req.UsStockStreamReq
import com.example.investfeed.domain.us.rank.dto.res.UsRankListRes
import com.example.investfeed.domain.us.rank.service.UsRankService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.US_STOCK_RANK)
@RestController
@RequestMapping("/api/us-stock/ranks")
class UsRankController(
    private val usRankService: UsRankService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listUsRanks(
        req: UsRankListReq
    ): ResponseEntity<ApiResponse<UsRankListRes>> {
        log.info { "listUsRanks : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_RANK_LIST.code,
                message = ResponseCode.US_RANK_LIST.message,
                result = usRankService.listUsRanks(req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamUsRanks(
        @RequestBody req: UsStockStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamUsRanks: $req" }

        usRankService.streamUsStocks(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_RANK_STREAM.code,
                message = ResponseCode.US_RANK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
