package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.MyBrokerAddReq
import com.example.investfeed.domain.holding.dto.res.BrokerListRes
import com.example.investfeed.domain.holding.dto.res.MyBrokerItem
import com.example.investfeed.domain.holding.dto.res.MyBrokerListRes
import com.example.investfeed.domain.holding.entity.MarketType
import com.example.investfeed.domain.holding.service.BrokerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/crypto/brokers")
class CryptoBrokerController(
    private val brokerService: BrokerService
) {

    @GetMapping
    fun listCryptoBrokers(): ResponseEntity<ApiResponse<BrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_BROKER_LIST.code,
                message = ResponseCode.CRYPTO_BROKER_LIST.message,
                result = brokerService.listBrokersByMarket(MarketType.CRYPTO)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/my")
    fun listMyCryptoBrokers(): ResponseEntity<ApiResponse<MyBrokerListRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MY_BROKER_LIST.code,
                message = ResponseCode.CRYPTO_MY_BROKER_LIST.message,
                result = brokerService.listMyBrokersByMarket(MarketType.CRYPTO)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/my")
    fun addMyCryptoBroker(
        @RequestBody req: MyBrokerAddReq
    ): ResponseEntity<ApiResponse<MyBrokerItem>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MY_BROKER_ADD.code,
                message = ResponseCode.CRYPTO_MY_BROKER_ADD.message,
                result = brokerService.addMyBroker(req)
            ), HttpStatus.OK
        )
    }

    @DeleteMapping("/my/{memberBrokerId}")
    fun removeMyCryptoBroker(
        @PathVariable memberBrokerId: Long
    ): ResponseEntity<ApiResponse<Nothing?>> {
        brokerService.removeMyBroker(memberBrokerId)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_MY_BROKER_REMOVE.code,
                message = ResponseCode.CRYPTO_MY_BROKER_REMOVE.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
