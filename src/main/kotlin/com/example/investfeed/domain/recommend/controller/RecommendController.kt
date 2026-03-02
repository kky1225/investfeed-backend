package com.example.investfeed.domain.recommend.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.kiwoom.exception.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/recommend")
class RecommendController(
    private val recommendService: RecommendService
) {
    @GetMapping("list")
    fun recommendList(): ResponseEntity<ApiResponse<RecommendListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RECOMMEND_LIST.code,
                message = ResponseCode.RECOMMEND_LIST.message,
                result = recommendService.recommandList()
            ), HttpStatus.OK
        )
    }
}