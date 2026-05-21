package com.example.investfeed.domain.recommend.marketmacro

import com.example.investfeed.kiwoom.sect.dto.res.KiwoomSectInvestor
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

data class MarketMacroSnapshot(
    val marketType: String,                  // KOSPI / KOSDAQ
    val priceChangeRate: BigDecimal,         // 당일 등락률 (실제 %, 예: -1.66)
    val institutionalNetBuy: Long,           // 기관계 순매수
    val foreignNetBuy: Long,                 // 외국인 순매수
    val updatedAt: LocalDateTime,
) {
    companion object {
        private val log = KotlinLogging.logger {}

        // ka10051(KiwoomSectInvestor) 의 flu_rt 는 0.01% 단위 정수 문자열로 내려옴
        // (예 "-166" = -1.66%). SectPriceNow.flu_rt(소수 "-1.66")와 포맷이 다름.
        private val SCALE_DIVISOR = BigDecimal(100)

        fun from(marketType: String, res: KiwoomSectInvestor): MarketMacroSnapshot {
            val rawFluRt = res.flu_rt
            val normalizedRate = rawFluRt.toBigDecimalOrNull()
                ?.divide(SCALE_DIVISOR, 2, RoundingMode.HALF_UP)
                ?: BigDecimal.ZERO
            return MarketMacroSnapshot(
                marketType = marketType,
                priceChangeRate = normalizedRate,
                institutionalNetBuy = res.orgn_netprps.toLongOrNull() ?: 0L,
                foreignNetBuy = res.frgnr_netprps.toLongOrNull() ?: 0L,
                updatedAt = LocalDateTime.now(),
            )
        }
    }
}
