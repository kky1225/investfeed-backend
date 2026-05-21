package com.example.investfeed.domain.papertrade.service

import org.springframework.stereotype.Service

enum class TrancheSide { BUY, SELL, NONE }

/** 이번 사이클에 낼 주문(시장가). qty 는 항상 ≥ 0, NONE 이면 0. */
data class TrancheOrder(val side: TrancheSide, val qty: Long)

/**
 * 등급 → 목표비중, 매 사이클 **고정스텝 선형 수렴** 트랜치 계산 (순수 함수).
 *
 * 확정 설계(첫 모의 런 config):
 * - 목표비중: STRONG_BUY/BUY = 종목당 W_MAX(20%) / SELL·STRONG_SELL = 0 / HOLD = 현 비중 동결.
 * - 분할: STRONG = 5사이클, BUY/SELL = 10사이클. 한 사이클 스텝 = (W_MAX × NAV) / 사이클수.
 *   매 사이클 *목표 방향으로 고정 금액*만큼 이동 → N사이클이면 풀 도달. 확신 = 채우는 속도.
 * - cycle_index 는 공식에 불필요(고정스텝이라 self-correcting: 한 사이클 누락돼도 다음 사이클이
 *   다시 한 스텝 전진, 드리프트 없음). cycle_index 는 paper_fill 에 기록만(사후 분석용, Phase 4).
 * - 비대칭("잃지 않기"): 매수는 한 주도 못 살 금액이면 그 사이클 건너뜀(노이즈 과확대 방지),
 *   매도는 잔량이 남아있으면 최소 1주라도 진행(청산은 반드시 완료되게).
 * - 현금 부족 시 STRONG 우선 배분은 *포트폴리오 횡단* 결정이라 여기 아님(Phase 4 ⑦).
 *   본 계산기는 종목별 *희망* 주문만 산출.
 */
@Service
class TrancheCalculator {
    companion object {
        const val W_MAX_RATIO = 0.20      // 종목당 최대 비중 (상한)
        const val STRONG_CYCLES = 5       // STRONG_BUY/STRONG_SELL 분할 사이클
        const val NORMAL_CYCLES = 10      // BUY/SELL 분할 사이클
    }

    /**
     * @param grade        보유평가/추천 최종 등급
     * @param currentQty   현재 보유 수량 (없으면 0)
     * @param price        1주 가격(원) — 사이징·환산용 (시장가 주문이라 직전 현재가 기준)
     * @param navTotal     총 포트폴리오 평가액(원) = 현금 + 보유평가 (W_MAX% 환산 기준)
     */
    fun calculate(grade: String, currentQty: Long, price: Long, navTotal: Long): TrancheOrder {
        if (price <= 0L || navTotal <= 0L) return TrancheOrder(TrancheSide.NONE, 0)

        val cycles = when (grade) {
            "STRONG_BUY", "STRONG_SELL" -> STRONG_CYCLES
            "BUY", "SELL" -> NORMAL_CYCLES
            else -> 0 // HOLD 등 → 동결
        }
        if (cycles == 0) return TrancheOrder(TrancheSide.NONE, 0)

        // 한 사이클 고정 스텝 금액 = (목표 최대치) / 사이클수 (매수·매도 동일 크기)
        val stepValue = (W_MAX_RATIO * navTotal / cycles).toLong()
        val currentValue = currentQty * price

        return when (grade) {
            "STRONG_BUY", "BUY" -> {
                val targetValue = (W_MAX_RATIO * navTotal).toLong()
                val room = targetValue - currentValue
                if (room <= 0L) return TrancheOrder(TrancheSide.NONE, 0) // 이미 목표 도달/초과
                val buyValue = minOf(stepValue, room)
                val qty = buyValue / price                              // floor, 한 주도 못 사면 0
                if (qty < 1L) TrancheOrder(TrancheSide.NONE, 0)
                else TrancheOrder(TrancheSide.BUY, qty)
            }

            "SELL", "STRONG_SELL" -> {
                if (currentQty <= 0L) return TrancheOrder(TrancheSide.NONE, 0)
                val sellValue = minOf(stepValue, currentValue)
                val rawQty = sellValue / price                          // floor
                // 청산은 반드시 진행: 스텝이 1주값보다 작아도 잔량 있으면 최소 1주
                val qty = minOf(currentQty, maxOf(1L, rawQty))
                TrancheOrder(TrancheSide.SELL, qty)
            }

            else -> TrancheOrder(TrancheSide.NONE, 0)
        }
    }
}
