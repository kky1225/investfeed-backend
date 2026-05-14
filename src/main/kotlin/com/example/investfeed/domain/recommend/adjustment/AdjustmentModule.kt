package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick

/**
 * 추천 보정 모듈 인터페이스 (Strategy Pattern).
 *
 * 점수제 보정에서 호출되며, 각 모듈은 자기 옵션 ON 여부([isEnabled])와
 * 격상/격하 판정([shouldPromote] / [shouldDemote])을 자체적으로 책임진다.
 *
 * 구현체는 모두 `@Component` 로 선언되어 Spring 이 자동으로 모아
 * `RecommendService` 에 `List<AdjustmentModule>` 로 주입한다. 신규 모듈 추가 시
 * 본 인터페이스만 구현하면 점수제에 자동 합류 — RecommendService 수정 불필요.
 *
 * 점수제 규칙은 [RecommendService.applyAdjustments] 참조.
 *   score = (격상 트리거 수) - (격하 트리거 수)
 *   score ≥ 1   → 한 단계 격상
 *   score ≤ -1  → 한 단계 격하
 *   score ≤ -3  → 두 단계 격하
 *   score == 0  → 유지
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
