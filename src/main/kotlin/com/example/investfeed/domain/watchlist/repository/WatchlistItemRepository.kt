package com.example.investfeed.domain.watchlist.repository

import com.example.investfeed.domain.watchlist.entity.WatchlistItem
import org.springframework.data.jpa.repository.JpaRepository

interface WatchlistItemRepository : JpaRepository<WatchlistItem, Long> {
    fun findByGroupIdOrderByDisplayOrderAsc(groupId: Long): List<WatchlistItem>
    fun findByGroupIdIn(groupIds: List<Long>): List<WatchlistItem>
    fun existsByGroupIdAndStkCd(groupId: Long, stkCd: String): Boolean
    fun deleteAllByGroupId(groupId: Long)
    fun countByGroupId(groupId: Long): Int
}
