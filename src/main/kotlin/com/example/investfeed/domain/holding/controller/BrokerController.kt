package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.MyBrokerAddReq
import com.example.investfeed.domain.holding.dto.res.BrokerListRes
import com.example.investfeed.domain.holding.dto.res.MyBrokerItem
import com.example.investfeed.domain.holding.dto.res.MyBrokerListRes
import com.example.investfeed.domain.holding.service.BrokerService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/stock/broker")
class BrokerController(
    private val brokerService: BrokerService
) {
    private val log = KotlinLogging.logger {}

    @GetMapping("list")
    fun brokerList(): ResponseEntity<ApiResponse<BrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.BROKER_LIST.code,
                message = ResponseCode.BROKER_LIST.message,
                result = brokerService.brokerList()
            ), HttpStatus.OK
        )
    }

    @GetMapping("my/list")
    fun myBrokerList(): ResponseEntity<ApiResponse<MyBrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MY_BROKER_LIST.code,
                message = ResponseCode.MY_BROKER_LIST.message,
                result = brokerService.myBrokerList()
            ), HttpStatus.OK
        )
    }

    @PostMapping("my/add")
    fun addMyBroker(
        @RequestBody req: MyBrokerAddReq
    ): ResponseEntity<ApiResponse<MyBrokerItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MY_BROKER_ADD.code,
                message = ResponseCode.MY_BROKER_ADD.message,
                result = brokerService.addMyBroker(req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("my/{memberBrokerId}")
    fun removeMyBroker(
        @PathVariable memberBrokerId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        brokerService.removeMyBroker(memberBrokerId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MY_BROKER_REMOVE.code,
                message = ResponseCode.MY_BROKER_REMOVE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
