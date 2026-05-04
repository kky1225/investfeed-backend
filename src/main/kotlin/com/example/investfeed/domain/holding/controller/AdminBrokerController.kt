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
import org.springframework.web.bind.annotation.*
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.ADMIN_BROKER)
@RestController
@RequestMapping("/api/admin/brokers")
class AdminBrokerController(
    private val adminBrokerService: AdminBrokerService
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listBrokers(): ResponseEntity<ApiResponse<BrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_LIST.code,
                message = ResponseCode.BROKER_LIST.message,
                result = adminBrokerService.listBrokers()
            ), HttpStatus.OK
        )
    }

    @PostMapping
    @RequiresAction(action = Actions.CREATE)
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

    @PutMapping("/{brokerId}")
    @RequiresAction(action = Actions.UPDATE)
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

    @DeleteMapping("/{brokerId}")
    @RequiresAction(action = Actions.DELETE)
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
