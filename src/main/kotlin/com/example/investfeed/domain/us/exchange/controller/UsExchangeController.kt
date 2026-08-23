package com.example.investfeed.domain.us.exchange.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.calendar.dto.res.IndicatorHistoryRes
import com.example.investfeed.domain.us.exchange.dto.res.UsExchangeRateRes
import com.example.investfeed.domain.us.exchange.service.UsExchangeService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.US_STOCK_EXCHANGE)
@RestController
@RequestMapping("/api/us-stock/exchange")
class UsExchangeController(
    private val usExchangeService: UsExchangeService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun getExchangeRate(): ResponseEntity<ApiResponse<UsExchangeRateRes>> {
        log.info { "getExchangeRate" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_EXCHANGE_RATE.code,
                message = ResponseCode.US_EXCHANGE_RATE.message,
                result = usExchangeService.getExchangeRate()
            ), HttpStatus.OK
        )
    }

    @GetMapping("/history")
    @RequiresAction(action = Actions.READ)
    fun getUsdKrwHistory(): ResponseEntity<ApiResponse<IndicatorHistoryRes?>> {
        log.info { "getUsdKrwHistory" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_EXCHANGE_HISTORY.code,
                message = ResponseCode.US_EXCHANGE_HISTORY.message,
                result = usExchangeService.getUsdKrwHistory()
            ), HttpStatus.OK
        )
    }
}
