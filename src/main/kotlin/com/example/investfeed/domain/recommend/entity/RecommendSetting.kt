package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "recommend_settings")
class RecommendSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_preset", nullable = false)
    var riskPreset: RiskPreset = RiskPreset.NORMAL,

    // 가격 변동성 보정 옵션 — STRONG 등급 종목이 단기 변동성 클 때 한 단계 격하 권유
    @Column(name = "price_volatility_enabled", nullable = false)
    var priceVolatilityEnabled: Boolean = false,

    // 이동평균선 보정 옵션 — 골든/데드크로스 발생 시 격상 권유
    @Column(name = "moving_average_enabled", nullable = false)
    var movingAverageEnabled: Boolean = false,

    // 코스피/코스닥 매크로 보정 옵션 — 시장 등락률 + 기관/외국인 매매 부호로 6가지 케이스 격상/격하
    @Column(name = "market_index_enabled", nullable = false)
    var marketIndexEnabled: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
