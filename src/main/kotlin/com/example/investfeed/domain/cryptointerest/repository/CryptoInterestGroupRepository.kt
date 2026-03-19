package com.example.investfeed.domain.cryptointerest.repository

import com.example.investfeed.domain.cryptointerest.entity.CryptoInterestGroup
import org.springframework.data.jpa.repository.JpaRepository

interface CryptoInterestGroupRepository : JpaRepository<CryptoInterestGroup, Long> {
    fun findByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<CryptoInterestGroup>
    fun countByMemberId(memberId: Long): Int
}
