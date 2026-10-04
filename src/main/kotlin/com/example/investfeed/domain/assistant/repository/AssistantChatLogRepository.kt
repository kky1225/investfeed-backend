package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantChatLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface AssistantChatLogRepository : JpaRepository<AssistantChatLog, Long> {
    fun existsByRequestId(requestId: String): Boolean
    fun findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start: LocalDateTime, end: LocalDateTime): List<AssistantChatLog>
    fun findByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(memberId: Long, start: LocalDateTime, end: LocalDateTime): List<AssistantChatLog>
}
