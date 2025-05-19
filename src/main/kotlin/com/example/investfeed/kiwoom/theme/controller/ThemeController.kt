package com.example.investfeed.kiwoom.theme.controller

import com.example.investfeed.kiwoom.config.ResponseCode
import com.example.investfeed.kiwoom.exception.ApiResponse
import com.example.investfeed.kiwoom.theme.dto.req.ThemeGroupListReq
import com.example.investfeed.kiwoom.theme.dto.req.ThemeGroupStockListReq
import com.example.investfeed.kiwoom.theme.dto.res.ThemeGroupListRes
import com.example.investfeed.kiwoom.theme.dto.res.ThemeGroupStockListRes
import com.example.investfeed.kiwoom.theme.service.ThemeService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/theme")
class ThemeController(
    private val themeService: ThemeService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("groupList")
    fun themeGroupList(
        req: ThemeGroupListReq
    ): ResponseEntity<ApiResponse<ThemeGroupListRes?>> {
        log.info { "themeGroupList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_GROUP_LIST.code,
                message = ResponseCode.THEME_GROUP_LIST.message,
                result = themeService.themeGroupList(req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("groupStockList")
    fun themeGroupStockList(
        req: ThemeGroupStockListReq
    ): ResponseEntity<ApiResponse<ThemeGroupStockListRes?>> {
        log.info { "themeGroupStockList $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_GROUP_STOCK_LIST.code,
                message = ResponseCode.THEME_GROUP_STOCK_LIST.message,
                result = themeService.themeGroupStockList(req)
            ), HttpStatus.OK
        )
    }
}