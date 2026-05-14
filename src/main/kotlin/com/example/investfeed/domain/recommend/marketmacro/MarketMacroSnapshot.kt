package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectInvestor
import java.math.BigDecimal
import java.time.LocalDateTime

data class MarketMacroSnapshot(
    val marketType: String,                  // KOSPI / KOSDAQ
    val priceChangeRate: BigDecimal,         // 당일 등락률 (%)
    val institutionalNetBuy: Long,           // 기관계 순매수
    val foreignNetBuy: Long,                 // 외국인 순매수
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(marketType: String, res: KiwoomSectInvestor): MarketMacroSnapshot {
            return MarketMacroSnapshot(
                marketType = marketType,
                priceChangeRate = res.flu_rt.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                institutionalNetBuy = res.orgn_netprps.toLongOrNull() ?: 0L,
                foreignNetBuy = res.frgnr_netprps.toLongOrNull() ?: 0L,
                updatedAt = LocalDateTime.now(),
            )
        }
    }
}
