package com.example.investfeed.domain.holding.controller

import com.example.investfeed.common.exception.ApiResponse
import com.example.investfeed.common.security.Actions
import com.example.investfeed.common.security.Permissions
import com.example.investfeed.common.security.RequiresAction
import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.holding.dto.req.HoldingStreamReq
import com.example.investfeed.domain.holding.dto.res.AssetDashboardRes
import com.example.investfeed.domain.holding.service.AssetDashboardService
import com.example.investfeed.domain.holding.service.CryptoHoldingService
import com.example.investfeed.domain.holding.service.HoldingService
import com.example.investfeed.domain.realizedpnl.service.RealizedPnlSummaryService
import com.example.investfeed.domain.goal.service.InvestmentGoalService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequiresAction(permission = Permissions.ASSET_DASHBOARD)
@RestController
@RequestMapping("/api/asset")
class AssetDashboardController(
    private val assetDashboardService: AssetDashboardService,
    private val realizedPnlSummaryService: RealizedPnlSummaryService,
    private val investmentGoalService: InvestmentGoalService,
    private val holdingService: HoldingService,
    private val cryptoHoldingService: CryptoHoldingService,
) {

    @GetMapping
    @RequiresAction(action = Actions.READ)
    fun getAssetDashboard(): ResponseEntity<ApiResponse<AssetDashboardRes>> {
        val result = assetDashboardService.getAssetDashboard()
        result.realizedPnl = realizedPnlSummaryService.getDashboardSummary()
        result.goals = investmentGoalService.getGoalsDashboard().goals

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ASSET_DASHBOARD.code,
                message = ResponseCode.ASSET_DASHBOARD.message,
                result = result
            ), HttpStatus.OK
        )
    }

    @PostMapping("/stocks/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamAssetStocks(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        holdingService.streamHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ASSET_STOCK_STREAM.code,
                message = ResponseCode.ASSET_STOCK_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }

    @PostMapping("/cryptos/stream")
    @RequiresAction(action = Actions.SUBSCRIBE)
    fun streamAssetCryptos(
        @RequestBody req: HoldingStreamReq
    ): ResponseEntity<ApiResponse<Nothing?>> {
        cryptoHoldingService.streamCryptoHoldings(req)

        return ResponseEntity(
            ApiResponse(
                code = ResponseCode.ASSET_CRYPTO_STREAM.code,
                message = ResponseCode.ASSET_CRYPTO_STREAM.message,
                result = null
            ), HttpStatus.OK
        )
    }
}
