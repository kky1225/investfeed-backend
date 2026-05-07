package com.example.investfeed.domain.recommend.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.recommend.dto.req.RecommendListStreamReq
import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequiresAction(permission = Permissions.STOCK_RECOMMEND)
@RestController
@RequestMapping("/api/stock/recommendations")
class RecommendController(
    private val recommendService: RecommendService
) {
    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listRecommendations(): ResponseEntity<ApiResponse<RecommendListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RECOMMEND_LIST.code,
                message = ResponseCode.RECOMMEND_LIST.message,
                result = recommendService.listRecommendations()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamRecommendations(
        @RequestBody req: RecommendListStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        recommendService.streamRecommendations(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RECOMMEND_LIST_STREAM.code,
                message = ResponseCode.RECOMMEND_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}