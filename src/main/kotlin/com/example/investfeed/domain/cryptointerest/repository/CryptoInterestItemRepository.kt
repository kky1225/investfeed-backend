package com.example.investfeed.domain.cryptointerest.repository

import com.example.investfeed.domain.cryptointerest.entity.CryptoInterestItem
import org.springframework.data.jpa.repository.JpaRepository

interface CryptoInterestItemRepository : JpaRepository<CryptoInterestItem, Long> {
    fun findByGroupIdOrderByDisplayOrderAsc(groupId: Long): List<CryptoInterestItem>
    fun findByGroupIdIn(groupIds: List<Long>): List<CryptoInterestItem>
    fun existsByGroupIdAndMarket(groupId: Long, market: String): Boolean
    fun deleteAllByGroupId(groupId: Long)
    fun countByGroupId(groupId: Long): Int
}
