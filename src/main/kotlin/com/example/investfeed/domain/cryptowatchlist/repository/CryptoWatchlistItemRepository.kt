package com.example.investfeed.domain.cryptowatchlist.repository

import com.example.investfeed.domain.cryptowatchlist.entity.CryptoWatchlistItem
import org.springframework.data.jpa.repository.JpaRepository

interface CryptoWatchlistItemRepository : JpaRepository<CryptoWatchlistItem, Long> {
    fun findByGroupIdOrderByDisplayOrderAsc(groupId: Long): List<CryptoWatchlistItem>
    fun findByGroupIdIn(groupIds: List<Long>): List<CryptoWatchlistItem>
    fun existsByGroupIdAndMarket(groupId: Long, market: String): Boolean
    fun deleteAllByGroupId(groupId: Long)
    fun countByGroupId(groupId: Long): Int
}
