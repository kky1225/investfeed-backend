package com.example.investfeed.domain.theme.controller

import com.example.investfeed.domain.theme.dto.req.ThemeListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListStreamReq
import com.example.investfeed.domain.theme.dto.res.ThemeListRes
import com.example.investfeed.domain.theme.dto.res.ThemeStockListRes
import com.example.investfeed.domain.theme.service.ThemeService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/theme")
class ThemeController(
    private val themeService: ThemeService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun themeList(
        req: ThemeListReq
    ): ResponseEntity<ApiResponse<ThemeListRes?>> {
        log.info { "themeList : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_LIST.code,
                message = ResponseCode.THEME_LIST.message,
                result = themeService.themeList(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("stock/list")
    fun themeStockList(
        req: ThemeStockListReq
    ): ResponseEntity<ApiResponse<ThemeStockListRes?>> {
        log.info { "themeStockList : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_STOCK_LIST.code,
                message = ResponseCode.THEME_STOCK_LIST.message,
                result = themeService.themeStockList(req = req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("stock/list/stream")
    fun themeStockStream(
        req: ThemeStockListStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "themeStockList : $req" }

        themeService.themeStockStream(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_STOCK_LIST_STREAM.code,
                message = ResponseCode.THEME_STOCK_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}