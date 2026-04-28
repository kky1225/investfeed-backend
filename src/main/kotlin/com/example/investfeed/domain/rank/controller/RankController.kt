package com.example.investfeed.domain.rank.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.rank.dto.req.RankListReq
import com.example.investfeed.domain.rank.dto.res.RankListRes
import com.example.investfeed.domain.rank.service.RankService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/stock/ranks")
class RankController(
    private val rankService: RankService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
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
}