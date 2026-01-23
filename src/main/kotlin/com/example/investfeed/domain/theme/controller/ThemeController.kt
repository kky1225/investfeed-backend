package com.example.investfeed.domain.theme.controller

import com.example.investfeed.domain.theme.dto.req.ThemeListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListReq
import com.example.investfeed.domain.theme.dto.res.ThemeListRes
import com.example.investfeed.domain.theme.dto.res.ThemeStockListRes
import com.example.investfeed.domain.theme.service.ThemeService
import com.example.investfeed.kiwoom.exception.ApiResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/theme")
class ThemeController(
    private val themeService: ThemeService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun themeList(
        req: ThemeListReq
    ): ResponseEntity<ApiResponse<ThemeListRes?>> {
        log.info { "themeList : $req" }

        return ResponseEntity(
            ApiResponse(
                code = "",
                message = "",
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
                code = "",
                message = "",
                result = themeService.themeStockList(req = req)
            ), HttpStatus.OK
        )
    }
}