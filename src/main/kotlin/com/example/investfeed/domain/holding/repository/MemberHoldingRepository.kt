package com.example.investfeed.domain.holding.repository

import com.example.investfeed.domain.holding.entity.MemberHolding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MemberHoldingRepository : JpaRepository<MemberHolding, Long> {
    @Modifying
    @Query("DELETE FROM MemberHolding m WHERE m.memberId = :memberId AND m.broker.id = :brokerId")
    fun deleteByMemberIdAndBrokerId(memberId: Long, brokerId: Long)

    fun findByMemberIdAndBrokerIdOrderByDisplayOrderAsc(memberId: Long, brokerId: Long): List<MemberHolding>
    fun findByMemberIdAndId(memberId: Long, id: Long): MemberHolding?

    @Query("SELECT COALESCE(MAX(m.displayOrder), -1) FROM MemberHolding m WHERE m.memberId = :memberId AND m.broker.id = :brokerId")
    fun findMaxDisplayOrder(memberId: Long, brokerId: Long): Int
}
