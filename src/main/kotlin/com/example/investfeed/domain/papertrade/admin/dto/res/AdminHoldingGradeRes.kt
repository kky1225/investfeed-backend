package com.example.investfeed.domain.papertrade.admin.dto.res

import java.time.LocalDate

data class AdminHoldingGradeRes(
    val evalDate: LocalDate?,
    val items: List<HoldingGradeItem>,
) {
    data class HoldingGradeItem(
        val stkCd: String,
        val stkNm: String,
        val type: String,             // STRONG_BUY / BUY / HOLD / SELL / STRONG_SELL
        val originSide: String?,      // BUY / SELL
        val marketType: String?,      // KOSPI / KOSDAQ
        val penfndK: Double?,
        val frgnrMcapRatio: Double?,
        // 결정 근거 (왜 이 등급/비중/사유인지)
        val frgnrOppositeK: Double?,    // 외국인 반대 K (BLOCK·freeze·부분비중 강도)
        val frgnrSameDirK: Double?,     // 외국인 동조 K (하드스톱)
        val priorTrendRatio: Double?,   // B′ 추세 명확성 (STRONG)
        val foreignerAligned: Boolean?, // 옵션B 외국인 동조
        val evaluationReason: String?,  // HARD_SELL / BLOCK_FREEZE / BLOCK_PARTIAL / CONFLICT (복수면 '|'), 없으면 null
        val targetWeightRatio: Double?, // 외국인 BLOCK 부분비중 0.10, 그 외 null(기본)
        // ─── 상세(팝업)용 — 모듈 보정 전 백본 + 백본사유 + 6개 후행 모듈 트리거 ───
        val preAdjustmentType: String?, // 모듈 보정 전 백본 등급 (HOLD→BUY 격상 추적)
        val backboneReason: String?,    // 백본 분류 사유 한 줄 (수급 근거)
        val pvTrigger: String?,         // PROMOTE / DEMOTE / NONE
        val maTrigger: String?,
        val vpTrigger: String?,
        val rsiTrigger: String?,
        val hl52wTrigger: String?,
        val breakoutTrigger: String?,
    )
}
