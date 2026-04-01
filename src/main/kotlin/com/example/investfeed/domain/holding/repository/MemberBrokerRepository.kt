package com.example.investfeed.domain.holding.repository

import com.example.investfeed.domain.holding.entity.MemberBroker
import org.springframework.data.jpa.repository.JpaRepository

interface MemberBrokerRepository : JpaRepository<MemberBroker, Long> {
    fun findByMemberIdOrderByOrderIndex(memberId: Long): List<MemberBroker>
    fun findByMemberIdAndId(memberId: Long, id: Long): MemberBroker?
    fun findByMemberIdAndBrokerId(memberId: Long, brokerId: Long): MemberBroker?
    fun countByMemberId(memberId: Long): Int
}
