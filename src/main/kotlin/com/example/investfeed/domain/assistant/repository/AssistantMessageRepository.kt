package com.example.investfeed.domain.assistant.repository

import com.example.investfeed.domain.assistant.entity.AssistantMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AssistantMessageRepository : JpaRepository<AssistantMessage, Long> {
    /** 확인 카드(목표가 알림·투자 목표 변경)가 실린 원래 비서 답변 찾기 — 본문 JSON 에 카드 ID(c_xxxx)가 들어 있는 최신 답변.
     *  [등록]/[취소] 처리 후 그 답변에 처리 상태를 기록해 새로고침해도 버튼이 다시 안 나오게 한다 */
    @Query(
        value = "SELECT * FROM assistant_message WHERE member_id = :memberId AND type = 'ASSISTANT' AND body::text LIKE CONCAT('%', :ref, '%') ORDER BY id DESC LIMIT 1",
        nativeQuery = true,
    )
    fun findAnswerContainingRef(@Param("memberId") memberId: Long, @Param("ref") ref: String): AssistantMessage?

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
