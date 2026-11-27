package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.StockTradeReq
import com.example.investfeed.domain.holding.dto.res.StockTradeRes
import com.example.investfeed.domain.holding.service.StockTradeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 한 종목의 내 매매. 주소가 /api/stock/holdings 아래라 보유 종목과 같이 2차 인증을 거친다.
 * 주소를 옮기면 2차 인증 대상에서 빠지므로 SecurityPaths.SECONDARY_AUTH_PATTERNS 를 함께 고칠 것.
 */
@RequiresAction(permission = Permissions.STOCK_HOLDINGS)
@RestController
@RequestMapping("/api/stock/holdings/trades")
class StockTradeController(
    private val stockTradeService: StockTradeService,
) {
    @GetMapping("/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getTrades(
        @PathVariable stkCd: String,
        req: StockTradeReq
    ): ResponseEntity<ApiResponse<StockTradeRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_TRADE_LIST.code,
                message = ResponseCode.STOCK_TRADE_LIST.message,
                result = stockTradeService.getTrades(stkCd, req)
            ), HttpStatus.OK
        )
    }
}
