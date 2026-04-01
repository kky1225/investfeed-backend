package com.example.investfeed.domain.holding.repository

import com.example.investfeed.domain.holding.entity.MemberHolding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MemberHoldingRepository : JpaRepository<MemberHolding, Long> {
    @Modifying
    @Query("DELETE FROM MemberHolding m WHERE m.memberId = :memberId AND m.provider = :provider")
    fun deleteByMemberIdAndProvider(memberId: Long, provider: String)
}
