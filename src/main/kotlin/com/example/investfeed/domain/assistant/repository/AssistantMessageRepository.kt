package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AssistantMessageRepository : JpaRepository<AssistantMessage, Long> {
    fun findByMemberIdAndIdLessThanOrderByIdDesc(memberId: Long, before: Long, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdOrderByIdDesc(memberId: Long, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdAndCreatedAtBetweenOrderByIdDesc(memberId: Long, start: LocalDateTime, end: LocalDateTime): List<AssistantMessage>
    fun findByMemberIdAndTypeInAndIdLessThanOrderByIdDesc(memberId: Long, types: Collection<String>, before: Long, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdAndTypeInOrderByIdDesc(memberId: Long, types: Collection<String>, pageable: Pageable): List<AssistantMessage>
    fun findByMemberIdAndTypeInAndCreatedAtBetweenOrderByIdDesc(memberId: Long, types: Collection<String>, start: LocalDateTime, end: LocalDateTime): List<AssistantMessage>
    fun countByMemberIdAndIdGreaterThanAndTypeIn(memberId: Long, lastSeenId: Long, types: Collection<String>): Long
    fun existsByMemberIdAndRefBriefingId(memberId: Long, refBriefingId: Long): Boolean
    fun findByIdAndMemberId(id: Long, memberId: Long): AssistantMessage?

    @Modifying
    @Query("delete from AssistantMessage m where m.memberId = :memberId and m.type in :types")
    fun deleteByMemberIdAndTypeIn(@Param("memberId") memberId: Long, @Param("types") types: Collection<String>): Int

    @Modifying
    @Query("delete from AssistantMessage m where m.memberId = :memberId")
    fun deleteAllByMemberId(@Param("memberId") memberId: Long): Int
    fun findFirstByMemberIdAndTypeAndSubtypeAndCreatedAtBetweenOrderByIdDesc(memberId: Long, type: String, subtype: String, start: LocalDateTime, end: LocalDateTime): AssistantMessage?
}
