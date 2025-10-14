package com.example.investfeed.kiwoom.gold.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.gold.dto.rest.req.GoldDetailReq
import com.example.investfeed.kiwoom.gold.dto.rest.res.GoldDetailRes
import com.example.investfeed.kiwoom.gold.service.GoldService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/gold")
class GoldController(
    val goldService: GoldService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("detail")
    fun goldDetail(
        req: GoldDetailReq,
    ): ResponseEntity<ApiResponse<GoldDetailRes<*>?>> {
        log.info { "indexDetail: $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.GOLD_DETAIL.code,
                message = ResponseCode.GOLD_DETAIL.message,
                result = goldService.goldDetail(req = req)
            ), HttpStatus.OK
        )
    }
}