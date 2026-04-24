package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.BrokerCreateReq
import com.example.investfeed.domain.holding.dto.req.BrokerUpdateReq
import com.example.investfeed.domain.holding.dto.res.BrokerItem
import com.example.investfeed.domain.holding.dto.res.BrokerListRes
import com.example.investfeed.domain.holding.service.AdminBrokerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/broker")
class AdminBrokerController(
    private val adminBrokerService: AdminBrokerService
) {

    @GetMapping("list")
    @PreAuthorize("hasRole('ADMIN')")
    fun brokerList(): ResponseEntity<ApiResponse<BrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_LIST.code,
                message = ResponseCode.BROKER_LIST.message,
                result = adminBrokerService.brokerList()
            ), HttpStatus.OK
        )
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createBroker(
        @Valid @RequestBody req: BrokerCreateReq
    ): ResponseEntity<ApiResponse<BrokerItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_CREATE.code,
                message = ResponseCode.BROKER_CREATE.message,
                result = adminBrokerService.createBroker(req)
            ), HttpStatus.OK
        )
    }

    @PutMapping("{brokerId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBroker(
        @PathVariable brokerId: Long,
        @Valid @RequestBody req: BrokerUpdateReq
    ): ResponseEntity<ApiResponse<BrokerItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_UPDATE.code,
                message = ResponseCode.BROKER_UPDATE.message,
                result = adminBrokerService.updateBroker(brokerId, req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("{brokerId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteBroker(
        @PathVariable brokerId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        adminBrokerService.deleteBroker(brokerId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_DELETE.code,
                message = ResponseCode.BROKER_DELETE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
