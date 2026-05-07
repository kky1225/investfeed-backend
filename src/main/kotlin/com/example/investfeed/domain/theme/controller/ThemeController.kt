package com.example.investfeed.domain.theme.controller

import com.example.investfeed.domain.theme.dto.req.ThemeListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListReq
import com.example.investfeed.domain.theme.dto.req.ThemeStockListStreamReq
import com.example.investfeed.domain.theme.dto.res.ThemeListRes
import com.example.investfeed.domain.theme.dto.res.ThemeStockListRes
import com.example.investfeed.domain.theme.service.ThemeService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.STOCK_THEME)
@RestController
@RequestMapping("/api/stock/themes")
class ThemeController(
    private val themeService: ThemeService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listThemes(
        req: ThemeListReq
    ): ResponseEntity<ApiResponse<ThemeListRes?>> {
        log.info { "listThemes : $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_LIST.code,
                message = ResponseCode.THEME_LIST.message,
                result = themeService.listThemes(req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{themaGrpCd}/stocks")
    @RequiresAction(action = Actions.READ)
    fun listStocksByTheme(
        @PathVariable themaGrpCd: String,
        req: ThemeStockListReq
    ): ResponseEntity<ApiResponse<ThemeStockListRes?>> {
        log.info { "listStocksByTheme : themaGrpCd=$themaGrpCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_STOCK_LIST.code,
                message = ResponseCode.THEME_STOCK_LIST.message,
                result = themeService.listStocksByTheme(themaGrpCd = themaGrpCd, req = req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stocks/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamThemeStocks(
        @RequestBody req: ThemeStockListStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamThemeStocks : $req" }

        themeService.streamThemeStocks(req = req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.THEME_STOCK_LIST_STREAM.code,
                message = ResponseCode.THEME_STOCK_LIST_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
