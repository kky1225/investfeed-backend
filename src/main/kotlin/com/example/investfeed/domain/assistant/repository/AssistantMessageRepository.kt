package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface AssistantMessageRepository : JpaRepository<AssistantMessage, Long> {
    fun findByMemberIdAndIdLessThanOrderByIdDesc(memberId: Long, before: Long, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdOrderByIdDesc(memberId: Long, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdAndCreatedAtBetweenOrderByIdDesc(memberId: Long, start: LocalDateTime, end: LocalDateTime): List<AssistantMessage>
    fun countByMemberIdAndIdGreaterThanAndTypeIn(memberId: Long, lastSeenId: Long, types: Collection<String>): Long
    fun existsByMemberIdAndRefBriefingId(memberId: Long, refBriefingId: Long): Boolean
}
