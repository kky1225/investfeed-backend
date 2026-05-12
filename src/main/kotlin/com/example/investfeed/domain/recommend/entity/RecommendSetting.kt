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

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
