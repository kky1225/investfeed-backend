package com.example.investfeed.domain.cryptowatchlist.repository

import com.example.investfeed.domain.cryptowatchlist.entity.CryptoWatchlistGroup
import org.springframework.data.jpa.repository.JpaRepository

interface CryptoWatchlistGroupRepository : JpaRepository<CryptoWatchlistGroup, Long> {
    fun findByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<CryptoWatchlistGroup>
    fun countByMemberId(memberId: Long): Int
}
