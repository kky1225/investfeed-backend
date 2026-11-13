package com.example.investfeed.domain.chart.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/** 회원별 차트 설정. 회원당 1행 */
@Entity
@Table(name = "chart_setting")
class ChartSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var settings: String,   // ChartSettingRes 를 JSON 으로 저장. 항목이 늘어도 컬럼을 바꾸지 않는다

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
