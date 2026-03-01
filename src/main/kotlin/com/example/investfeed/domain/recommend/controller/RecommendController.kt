package com.example.investfeed.domain.recommend.controller

import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.kiwoom.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/recommand")
class RecommendController(
    private val recommendService: RecommendService
) {
    @GetMapping("list")
    fun recommandList(): ResponseEntity<ApiResponse<RecommendListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = "",
                message = "",
                result = recommendService.recommandList()
            ), HttpStatus.OK
        )
    }
}