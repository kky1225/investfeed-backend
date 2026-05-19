package com.example.investfeed.domain.recommend.adjustment

import com.example.investfeed.domain.recommend.Position
import com.example.investfeed.domain.recommend.entity.RecommendSetting
import com.example.investfeed.domain.recommend.entity.StockPick

/**
 * 추천 보정 모듈 인터페이스 (Strategy Pattern).
 *
 * 다수결 보정에서 호출되며, 각 모듈은 자기 옵션 ON 여부([isEnabled])와
 * 격상/격하 판정([shouldPromote] / [shouldDemote])을 자체적으로 책임진다.
 *
 * 구현체는 모두 `@Component` 로 선언되어 Spring 이 자동으로 모아
 * `RecommendService` 에 `List<AdjustmentModule>` 로 주입한다. 신규 모듈 추가 시
 * 본 인터페이스만 구현하면 다수결에 자동 합류 — RecommendService 수정 불필요.
 *
 * 다수결 규칙은 [RecommendService.resolveStage1] 참조.
 *   격상: 격상풀 과반 + 격하표 0  → 한 단계 격상
 *   격하: 격하풀 전원 + 격상표 0  → 두 단계 격하 (만장일치)
 *   격하: 격하풀 과반 + 격상표 0  → 한 단계 격하 (다수결)
 *   그 외 (반대표 1개라도 존재 / 과반 미달) → 유지
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

    /**
     * 격상 능력 여부 — 격하 전용 모듈은 false 오버라이드. 기본 true.
     *
     * 다수결 판정 시 "격상 능력 있는 활성 모듈" 집합(격상풀)을 계산하는 데 사용.
     * 격상 능력이 본질적으로 없는 모듈은 격상풀에서 제외되어,
     * 분모를 부풀려 과반/만장일치 달성을 영원히 막는 문제를 방지.
     */
    fun canPromote(side: Position): Boolean = true

    /**
     * 격하 능력 여부 — 격상 전용 모듈은 false 오버라이드. 기본 true.
     *
     * 다수결 판정 시 격하풀 계산에 동일 목적으로 사용.
     */
    fun canDemote(side: Position): Boolean = true
}
