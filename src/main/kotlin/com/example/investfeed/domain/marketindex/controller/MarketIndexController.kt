package com.example.investfeed.domain.marketindex.controller

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.BitcoinSummary
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexDashboardRes
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import com.example.investfeed.domain.crypto.service.CryptoService
import com.example.investfeed.upbit.ticker.client.TickerClient
import com.example.investfeed.upbit.ticker.dto.res.UpbitTickerRes
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/market-indexes")
class MarketIndexController(
    private val marketIndexService: MarketIndexService,
    private val cryptoService: CryptoService,
    private val tickerClient: TickerClient,
) {

    @GetMapping
    fun listMarketIndexes(): ResponseEntity<MarketIndexDashboardRes> {
        val indices = marketIndexService.listMarketIndexes()
        val fearGreed = cryptoService.fearGreedIndex()

        // BTC, ETH 를 한 번의 Upbit 호출로 동시 조회 (markets 파라미터 콤마 구분)
        val tickers = tickerClient.getTickers("KRW-BTC,KRW-ETH").associateBy { it.market }
        val bitcoin = tickers["KRW-BTC"]?.toSummary()
        val ethereum = tickers["KRW-ETH"]?.toSummary()

        return ResponseEntity.ok(
            MarketIndexDashboardRes(
                indices = indices,
                fearGreed = fearGreed,
                bitcoin = bitcoin,
                ethereum = ethereum,
            )
        )
    }

    @GetMapping("/{type}")
    fun getMarketIndex(@PathVariable type: MarketIndexType): ResponseEntity<MarketIndexRes> {
        val result = marketIndexService.getMarketIndex(type)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    private fun UpbitTickerRes.toSummary(): BitcoinSummary {
        val changePrice = signed_change_price?.toLong() ?: 0
        val changeRate = (signed_change_rate ?: 0.0) * 100
        val trend = when {
            changePrice > 0 -> "UP"
            changePrice < 0 -> "DOWN"
            else -> "EVEN"
        }
        return BitcoinSummary(
            price = (trade_price?.toLong() ?: 0).toString(),
            changeAmount = changePrice.toString(),
            changeRate = String.format("%.2f", changeRate),
            trend = trend,
        )
    }
}
