package com.example.investfeed.domain.us.stock.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.us.stock.dto.req.UsStockSearchReq
import com.example.investfeed.domain.us.stock.dto.res.UsStockSearchItem
import com.example.investfeed.domain.us.stock.service.UsStockInfoService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.US_STOCK_INFO)
@RestController
@RequestMapping("/api/us-stock/stocks")
class UsStockController(
    private val usStockInfoService: UsStockInfoService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun searchUsStocks(
        req: UsStockSearchReq
    ): ResponseEntity<ApiResponse<List<UsStockSearchItem>>> {
        log.info { "searchUsStocks: ${req.keyword}" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_STOCK_SEARCH.code,
                message = ResponseCode.US_STOCK_SEARCH.message,
                result = usStockInfoService.searchUsStocks(req.keyword)
            ), HttpStatus.OK
        )
    }
}
