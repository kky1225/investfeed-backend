package com.example.investfeed.domain.commodity.controller

import com.example.investfeed.domain.commodity.dto.req.CommodityDetailReq
import com.example.investfeed.domain.commodity.dto.req.CommodityStreamReq
import com.example.investfeed.domain.commodity.dto.res.CommodityDetailRes
import com.example.investfeed.domain.commodity.dto.res.CommodityListRes
import com.example.investfeed.domain.commodity.service.CommodityService
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.kiwoom.realtime.client.RealTimeClient
import com.example.investfeed.kiwoom.realtime.dto.KiwoomGoldPriceStream
import com.example.investfeed.kiwoom.realtime.dto.KiwoomGoldPriceStreamReq
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction

@RequiresAction(permission = Permissions.COMMODITY)
@RestController
@RequestMapping("/api/commodities")
class CommodityController(
    private val commodityService: CommodityService,
    private val realTimeClient: RealTimeClient,
) {
    private val log = KotlinLogging.logger {}

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun listCommodities(): ResponseEntity<ApiResponse<CommodityListRes>> {
        log.info { "listCommodities" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.COMMODITY_LIST.code,
                message = ResponseCode.COMMODITY_LIST.message,
                result = commodityService.listCommodities()
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamCommodities(
        @RequestBody req: CommodityStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        log.info { "streamCommodities: $req" }

        realTimeClient.goldPriceListStream(
            req = KiwoomGoldPriceStreamReq(
                trnm = "REG",
                grp_no = "0001",
                refresh = "0",
                data = listOf(
                    KiwoomGoldPriceStream(
                        item = req.items,
                        type = listOf("0B")
                    )
                )
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.COMMODITY_STREAM.code,
                message = ResponseCode.COMMODITY_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @GetMapping("/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getCommodity(
        @PathVariable stkCd: String,
        req: CommodityDetailReq
    ): ResponseEntity<ApiResponse<CommodityDetailRes>> {
        log.info { "getCommodity: stkCd=$stkCd, $req" }

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.COMMODITY_DETAIL.code,
                message = ResponseCode.COMMODITY_DETAIL.message,
                result = commodityService.getCommodity(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }
}
