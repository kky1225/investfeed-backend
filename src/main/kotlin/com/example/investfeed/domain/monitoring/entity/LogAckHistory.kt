package com.example.investfeed.domain.monitoring.entity

import jakarta.persistence.*
import java.time.LocalDateTime

enum class AckSourceType { SCHEDULER_LOG, ERROR_LOG }
enum class AckAction { ACKNOWLEDGE, EDIT_NOTE, CANCEL, BULK_ACKNOWLEDGE }

/**
 * 로그 acknowledge 이력.
 *
 * - scheduler_log / error_log 모두 대응 (source_type 으로 구분)
 * - ACKNOWLEDGE: 최초 확인 처리 시 기록
 * - EDIT_NOTE: 이미 확인된 상태에서 사유 수정
 * - CANCEL: 확인 취소
 * - BULK_ACKNOWLEDGE: 미확인 전체 일괄 확인 처리 (사유 누락 시 "일괄 확인" 디폴트)
 */
@Entity
@Table(name = "log_ack_history")
class LogAckHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val sourceType: AckSourceType,

    @Column(nullable = false)
    val sourceId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val action: AckAction,

    @Column(columnDefinition = "TEXT")
    val oldNote: String? = null,

    @Column(columnDefinition = "TEXT")
    val newNote: String? = null,

    @Column(nullable = false)
    val actedBy: Long,

    @Column(nullable = false)
    val actedAt: LocalDateTime = LocalDateTime.now(),
)
