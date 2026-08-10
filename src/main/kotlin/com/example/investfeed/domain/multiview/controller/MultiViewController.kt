package com.example.investfeed.domain.multiview.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.commodity.dto.req.CommodityDetailReq
import com.example.investfeed.domain.commodity.dto.res.CommodityDetailRes
import com.example.investfeed.domain.commodity.service.CommodityService
import com.example.investfeed.domain.crypto.dto.req.CryptoDetailReq
import com.example.investfeed.domain.crypto.dto.res.CryptoDetailRes
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.domain.multiview.dto.req.MultiViewStreamReq
import com.example.investfeed.domain.multiview.dto.req.MultiViewUsStreamReq
import com.example.investfeed.domain.stock.dto.req.StockDetailReq
import com.example.investfeed.domain.stock.dto.req.StockStreamReq
import com.example.investfeed.domain.stock.dto.res.StockChartRes
import com.example.investfeed.domain.stock.service.StockService
import com.example.investfeed.domain.us.rank.dto.req.UsStockStreamItem
import com.example.investfeed.domain.us.rank.dto.req.UsStockStreamReq
import com.example.investfeed.domain.us.rank.service.UsRankService
import com.example.investfeed.domain.us.stock.dto.req.UsStockDetailReq
import com.example.investfeed.domain.us.stock.dto.res.UsStockChartRes
import com.example.investfeed.domain.us.stock.service.UsStockInfoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.MULTI_VIEW)
@RestController
@RequestMapping("/api/multi-view")
class MultiViewController(
    private val stockService: StockService,
    private val cryptoService: CryptoService,
    private val commodityService: CommodityService,
    private val usStockInfoService: UsStockInfoService,
    private val usRankService: UsRankService,
) {

    @GetMapping("/charts/stock/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getStockChart(
        @PathVariable stkCd: String,
        req: StockDetailReq,
    ): ResponseEntity<ApiResponse<StockChartRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.STOCK_DETAIL.code,
                message = ResponseCode.STOCK_DETAIL.message,
                result = stockService.getStockChart(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/charts/us-stock/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getUsStockChart(
        @PathVariable stkCd: String,
        req: UsStockDetailReq,
    ): ResponseEntity<ApiResponse<UsStockChartRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.US_STOCK_DETAIL.code,
                message = ResponseCode.US_STOCK_DETAIL.message,
                result = usStockInfoService.getUsStockChart(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }

    @GetMapping("/charts/crypto/{market}")
    @RequiresAction(action = Actions.READ)
    fun getCrypto(
        @PathVariable market: String,
        req: CryptoDetailReq,
    ): ResponseEntity<ApiResponse<CryptoDetailRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.CRYPTO_DETAIL.code,
                message = ResponseCode.CRYPTO_DETAIL.message,
                result = cryptoService.getCrypto(market = market, req = req)
            ), HttpStatus.OK
        )
    }

    /**
     * MultiView 페이지에서 호출하는 원자재 상세 조회.
     */
    @GetMapping("/charts/commodity/{stkCd}")
    @RequiresAction(action = Actions.READ)
    fun getCommodity(
        @PathVariable stkCd: String,
        req: CommodityDetailReq,
    ): ResponseEntity<ApiResponse<CommodityDetailRes>> {
        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.COMMODITY_DETAIL.code,
                message = ResponseCode.COMMODITY_DETAIL.message,
                result = commodityService.getCommodity(stkCd = stkCd, req = req)
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stocks/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamMultiViewStocks(
        @RequestBody req: MultiViewStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        stockService.streamStocks(StockStreamReq(items = req.items))

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MULTI_VIEW_STOCK_STREAM.code,
                message = ResponseCode.MULTI_VIEW_STOCK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/us-stocks/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamMultiViewUsStocks(
        @RequestBody req: MultiViewUsStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        usRankService.streamUsStocks(
            UsStockStreamReq(
                items = req.items.map {
                    UsStockStreamItem(
                        stkCd = it.stkCd,
                        stexTp = it.stexTp
                    )
                }
            )
        )

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MULTI_VIEW_US_STOCK_STREAM.code,
                message = ResponseCode.MULTI_VIEW_US_STOCK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/cryptos/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamMultiViewCryptos(
        @RequestBody req: MultiViewStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoService.streamCryptos(req.items)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.MULTI_VIEW_CRYPTO_STREAM.code,
                message = ResponseCode.MULTI_VIEW_CRYPTO_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
