package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick

/**
 * 추천 보정 모듈 인터페이스 (Strategy Pattern).
 */
interface AdjustmentModule {
    /** 로깅/디버깅용 모듈 식별자. */
    val name: String

    /** 사용자가 옵션 ON 한 상태인지 — 각 모듈이 자기 설정 필드를 직접 확인. */
    fun isEnabled(setting: RecommendSetting): Boolean

    /** 한 단계 격상 트리거 조건 충족 여부. */
    fun shouldPromote(pick: StockPick, side: Position): Boolean

    /** 한 단계 격하 트리거 조건 충족 여부. */
    fun shouldDemote(pick: StockPick, side: Position): Boolean
}
