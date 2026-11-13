package com.example.investfeed.domain.chart.entity

import com.example.investfeed.domain.notification.entity.AssetType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/** 회원이 차트에 그린 선. 회원 + 종목당 1행에 선 전체를 담는다 */
@Entity
@Table(name = "chart_drawing")
class ChartDrawing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    val assetType: AssetType,

    @Column(name = "asset_code", nullable = false, length = 50)
    val assetCode: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var drawings: String,   // List<ChartDrawingItem> 을 JSON 으로 저장

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
