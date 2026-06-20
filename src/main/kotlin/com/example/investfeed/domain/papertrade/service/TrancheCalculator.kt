package com.example.investfeed.domain.papertrade.service

import org.springframework.stereotype.Service

enum class TrancheSide { BUY, SELL, NONE }

/** 이번 사이클에 낼 주문(시장가). qty 는 항상 ≥ 0, NONE 이면 0. */
data class TrancheOrder(val side: TrancheSide, val qty: Long)

@Service
class TrancheCalculator {
    companion object {
        const val W_MAX_RATIO = 0.10
        const val VOL_REF = 0.25
        const val VOL_FLOOR = 0.05
        const val STRONG_BUY_CYCLES = 3
        const val BUY_CYCLES = 5
        const val STRONG_SELL_CYCLES = 2
        const val SELL_CYCLES = 3

        fun volCap(realizedVol: Double?): Double {
            if (realizedVol == null || realizedVol <= 0.0) return W_MAX_RATIO
            return (W_MAX_RATIO * VOL_REF / realizedVol).coerceIn(VOL_FLOOR, W_MAX_RATIO)
        }
    }

    /**
     * @param grade        보유평가/추천 최종 등급
     * @param currentQty   현재 보유 수량 (없으면 0)
     * @param price        1주 가격(원) — 사이징·환산용 (시장가 주문이라 직전 현재가 기준)
     * @param navTotal     총 포트폴리오 평가액(원) = 현금 + 보유평가
     * @param targetRatio  목표 비중. 매수=상한(cap), 매도=하한(floor). null 이면 기본(매수 [W_MAX_RATIO], 매도 0).
     *                     변동성 캡([volCap]) · 외국인 BLOCK 부분비중 등 호출부에서 주입. 스텝 밴드도 이 목표로 결정.
     */
    fun calculate(
        grade: String,
        currentQty: Long,
        price: Long,
        navTotal: Long,
        targetRatio: Double? = null,
    ): TrancheOrder {
        if (price <= 0L || navTotal <= 0L) return TrancheOrder(TrancheSide.NONE, 0)
        val currentValue = currentQty * price

        return when (grade) {
            "STRONG_BUY", "BUY" -> {
                val cap = targetRatio ?: W_MAX_RATIO                 // 매수 목표 비중(상한)
                val cycles = if (grade == "STRONG_BUY") STRONG_BUY_CYCLES else BUY_CYCLES
                val targetValue = (cap * navTotal).toLong()
                val room = targetValue - currentValue
                if (room <= 0L) return TrancheOrder(TrancheSide.NONE, 0) // 이미 목표 도달/초과
                val stepValue = (cap * navTotal / cycles).toLong()
                val buyValue = minOf(stepValue, room)
                val qty = buyValue / price                           // floor, 한 주도 못 사면 0
                if (qty < 1L) TrancheOrder(TrancheSide.NONE, 0)
                else TrancheOrder(TrancheSide.BUY, qty)
            }

            "SELL", "STRONG_SELL" -> {
                if (currentQty <= 0L) return TrancheOrder(TrancheSide.NONE, 0)
                val floor = targetRatio ?: 0.0                       // 매도 목표 비중(하한)
                val cycles = if (grade == "STRONG_SELL") STRONG_SELL_CYCLES else SELL_CYCLES
                val floorValue = (floor * navTotal).toLong()
                val excess = currentValue - floorValue               // 하한 위로 남은 양
                if (excess <= 0L) return TrancheOrder(TrancheSide.NONE, 0) // 이미 하한 이하 → 보유
                val stepValue = ((W_MAX_RATIO - floor) * navTotal / cycles).toLong()
                val sellValue = minOf(stepValue, excess)
                val rawQty = sellValue / price                       // floor
                val qty = minOf(currentQty, maxOf(1L, rawQty))
                TrancheOrder(TrancheSide.SELL, qty)
            }

            "HARD_SELL" -> {
                if (currentQty <= 0L) TrancheOrder(TrancheSide.NONE, 0)
                else TrancheOrder(TrancheSide.SELL, currentQty)
            }

            else -> TrancheOrder(TrancheSide.NONE, 0)
        }
    }
}
