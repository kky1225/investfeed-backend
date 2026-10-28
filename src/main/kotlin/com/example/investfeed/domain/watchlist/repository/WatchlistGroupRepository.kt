package com.example.investfeed.domain.watchlist.repository

import com.example.investfeed.domain.watchlist.entity.WatchlistGroup
import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistGroupRepository : JpaRepository<WatchlistGroup, Long> {
    fun findByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<WatchlistGroup>
    fun countByMemberId(memberId: Long): Int
}
