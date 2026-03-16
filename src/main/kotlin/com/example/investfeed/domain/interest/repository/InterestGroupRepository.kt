package com.example.investfeed.domain.interest.repository

import com.example.investfeed.domain.interest.entity.InterestGroup
import org.springframework.data.jpa.repository.JpaRepository

interface InterestGroupRepository : JpaRepository<InterestGroup, Long> {
    fun findByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<InterestGroup>
    fun countByMemberId(memberId: Long): Int
}
