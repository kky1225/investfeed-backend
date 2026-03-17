package com.example.investfeed.domain.marketindex.controller

import com.example.investfeed.domain.marketindex.MarketIndexType
import com.example.investfeed.domain.marketindex.dto.res.MarketIndexRes
import com.example.investfeed.domain.marketindex.service.MarketIndexService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/market-index")
class MarketIndexController(
    private val marketIndexService: MarketIndexService,
) {

    @GetMapping
    fun getAll(): ResponseEntity<List<MarketIndexRes>> {
        return ResponseEntity.ok(marketIndexService.getAll())
    }

    @GetMapping("/{type}")
    fun getByType(@PathVariable type: MarketIndexType): ResponseEntity<MarketIndexRes> {
        val result = marketIndexService.getByType(type)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }
}
