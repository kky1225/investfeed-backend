package com.example.investfeed.domain.papertrade.service

import org.springframework.stereotype.Service

/**
 * 모의 성과·벤치마크 순수 계산 (DB/키움 의존 0 — 단위테스트 대상).
 */
@Service
class PaperTradeReportCalculator {

    /** start→end 수익률(%). start ≤ 0 이면 0. */
    fun pctReturn(start: Double, end: Double): Double =
        if (start <= 0.0) 0.0 else (end - start) / start * 100.0
}
