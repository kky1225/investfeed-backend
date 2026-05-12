package com.example.investfeed.domain.recommend.controller

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.recommend.dto.req.RecommendListStreamReq
import com.example.investfeed.domain.recommend.dto.req.RecommendSettingReq
import com.example.investfeed.domain.recommend.dto.res.RecommendListRes
import com.example.investfeed.domain.recommend.dto.res.RecommendSettingRes
import com.example.investfeed.domain.recommend.service.RecommendSettingService
import com.example.investfeed.domain.recommend.service.RecommendService
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RequiresAction(permission = Permissions.STOCK_RECOMMEND)
@RestController
@RequestMapping("/api/stock/recommendations")
class RecommendController(
    private val recommendService: RecommendService,
    private val recommendSettingService: RecommendSettingService,
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

    @GetMapping("/settings")
    @RequiresAction(action = Actions.READ)
    fun getRecommendSetting(): ResponseEntity<ApiResponse<RecommendSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RECOMMEND_SETTING.code,
                message = ResponseCode.RECOMMEND_SETTING.message,
                result = recommendSettingService.getSetting()
            ), HttpStatus.OK
        )
    }

    @PutMapping("/settings")
    @RequiresAction(action = Actions.UPDATE)
    fun saveRecommendSetting(
        @RequestBody req: RecommendSettingReq
    ): ResponseEntity<ApiResponse<RecommendSettingRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.RECOMMEND_SETTING_SAVE.code,
                message = ResponseCode.RECOMMEND_SETTING_SAVE.message,
                result = recommendSettingService.saveSetting(req)
            ), HttpStatus.OK
        )
    }
}