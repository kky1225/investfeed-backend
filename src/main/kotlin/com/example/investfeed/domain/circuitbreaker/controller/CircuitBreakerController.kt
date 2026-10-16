package com.example.investfeed.domain.circuitbreaker.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.circuitbreaker.dto.res.CircuitBreakerRes
import com.example.investfeed.domain.circuitbreaker.service.CircuitBreakerService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.STOCK_INDEX)
@RestController
@RequestMapping("/api/stock/indexes")
class CircuitBreakerController(
    private val circuitBreakerService: CircuitBreakerService,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("/{indsCd}/circuit-breakers")
    @RequiresAction(action = Actions.READ)
    fun getCircuitBreakers(@PathVariable indsCd: String): ResponseEntity<ApiResponse<List<CircuitBreakerRes>>> {
        log.info { "getCircuitBreakers: $indsCd" }

        val indexCode = CircuitBreakerService.INDS_CD[indsCd]
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CIRCUIT_BREAKER_LIST.code,
                message = ResponseCode.CIRCUIT_BREAKER_LIST.message,
                result = indexCode?.let { code -> circuitBreakerService.findToday().filter { it.indexCode == code } }.orEmpty()
            ), HttpStatus.OK
        )
    }
}
