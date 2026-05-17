package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일일 매크로(코스피/코스닥) 스냅샷.
 *
 * 22:00 RecommendScheduler 가 매크로 보정 적용 직후 1행 저장. 백테스트/디버깅 시
 * 그날의 매크로 상황 (등락률, 외인/기관 매매 부호, 6가지 케이스 분류) 을 재현하는 데 사용.
 */
@Entity
@Table(name = "market_index_snapshot")
class MarketIndexSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "captured_date", nullable = false, unique = true)
    val capturedDate: LocalDate,

    // KOSPI
    @Column(name = "kospi_change_rate")
    val kospiChangeRate: Double? = null,

    @Column(name = "kospi_foreigner_sign")
    val kospiForeignerSign: String? = null,

    @Column(name = "kospi_institution_sign")
    val kospiInstitutionSign: String? = null,

    @Column(name = "kospi_scenario")
    val kospiScenario: String? = null,

    // KOSDAQ
    @Column(name = "kosdaq_change_rate")
    val kosdaqChangeRate: Double? = null,

    @Column(name = "kosdaq_foreigner_sign")
    val kosdaqForeignerSign: String? = null,

    @Column(name = "kosdaq_institution_sign")
    val kosdaqInstitutionSign: String? = null,

    @Column(name = "kosdaq_scenario")
    val kosdaqScenario: String? = null,

    @Column(name = "captured_at", nullable = false)
    val capturedAt: LocalDateTime = LocalDateTime.now(),
)
