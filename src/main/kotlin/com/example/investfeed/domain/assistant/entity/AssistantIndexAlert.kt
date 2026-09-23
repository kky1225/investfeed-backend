package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "assistant_index_alert")
class AssistantIndexAlert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "alert_date", nullable = false)
    val alertDate: LocalDate,                 // 국내=KST 일자, 미국=미국 거래일

    @Column(name = "index_code", nullable = false, length = 20)
    val indexCode: String,                    // KOSPI / KOSDAQ / NASDAQ / SP500

    @Column(nullable = false, length = 20)
    val subtype: String,                      // INDEX_WARN / INDEX_CB

    @Column(name = "trigger_type", nullable = false, length = 20)
    val triggerType: String,                  // THRESHOLD(±3%·±5% 도달) / CB

    @Column(nullable = false, length = 4)
    val direction: String,                    // UP / DOWN

    val stage: Int? = null,                   // 서킷브레이커 단계

    @Column(name = "trigger_rate", nullable = false)
    val triggerRate: Double,                  // 발동 근거 등락률 (고가·저가, 국내 CB 는 현재값)

    @Column(name = "current_rate", nullable = false)
    val currentRate: Double,

    @Column(name = "index_value")
    val indexValue: Double? = null,

    @Column(name = "fired_at", nullable = false)
    val firedAt: LocalDateTime,

    @Column(name = "headline_text", nullable = false, length = 300)
    val headlineText: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val body: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
