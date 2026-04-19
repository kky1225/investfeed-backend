package com.example.investfeed.domain.marketindex.controller

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.BitcoinSummary
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexDashboardRes
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.upbit.ticker.client.TickerClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/market-index")
class MarketIndexController(
    private val marketIndexService: MarketIndexService,
    private val cryptoService: CryptoService,
    private val tickerClient: TickerClient,
) {

    @GetMapping
    fun getAll(): ResponseEntity<MarketIndexDashboardRes> {
        val indices = marketIndexService.getAll()
        val fearGreed = cryptoService.fearGreedIndex()

        val btcTicker = tickerClient.getTickers("KRW-BTC").firstOrNull()
        val bitcoin = btcTicker?.let {
            val changePrice = it.signed_change_price?.toLong() ?: 0
            val changeRate = (it.signed_change_rate ?: 0.0) * 100
            val trend = when {
                changePrice > 0 -> "UP"
                changePrice < 0 -> "DOWN"
                else -> "EVEN"
            }
            BitcoinSummary(
                price = (it.trade_price?.toLong() ?: 0).toString(),
                changeAmount = changePrice.toString(),
                changeRate = String.format("%.2f", changeRate),
                trend = trend,
            )
        }

        return ResponseEntity.ok(MarketIndexDashboardRes(indices = indices, fearGreed = fearGreed, bitcoin = bitcoin))
    }

    @GetMapping("/{type}")
    fun getByType(@PathVariable type: MarketIndexType): ResponseEntity<MarketIndexRes> {
        val result = marketIndexService.getByType(type)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }
}
