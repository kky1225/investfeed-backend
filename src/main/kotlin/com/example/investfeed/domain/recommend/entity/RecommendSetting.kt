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

    // 보정 옵션 기본값 = 모두 ON (사용자 철학: "잃지 않기" — 다층 안전망 디폴트 활성화).
    // 만장일치 룰이라 모든 옵션 ON 이어도 발동 빈도 보수적.

    // 가격 변동성 보정 옵션 — STRONG 등급 종목이 단기 변동성 클 때 한 단계 격하 권유
    @Column(name = "price_volatility_enabled", nullable = false)
    var priceVolatilityEnabled: Boolean = true,

    // 이동평균선 보정 옵션 — 골든/데드크로스 발생 시 격상 권유
    @Column(name = "moving_average_enabled", nullable = false)
    var movingAverageEnabled: Boolean = true,

    // 코스피/코스닥 매크로 보정 옵션 — 시장 등락률 + 기관/외국인 매매 부호로 6가지 케이스 격상/격하
    @Column(name = "market_index_enabled", nullable = false)
    var marketIndexEnabled: Boolean = true,

    // 거래량-가격 보정 옵션 — 종목 당일 등락률 + RVOL(20일 평균 대비) 4분면 매트릭스
    @Column(name = "volume_price_enabled", nullable = false)
    var volumePriceEnabled: Boolean = true,

    // RSI 보정 옵션 — 14일 RSI 50선 기준 + 70 이탈 3일 확정 추세 추종 활용
    @Column(name = "rsi_enabled", nullable = false)
    var rsiEnabled: Boolean = true,

    // 52주 위치 보정 옵션 — Stage Analysis 기반 (저점 +15% 반등 매수 / 고점 -15% 하락 매도)
    @Column(name = "high_low_52w_enabled", nullable = false)
    var highLow52wEnabled: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
