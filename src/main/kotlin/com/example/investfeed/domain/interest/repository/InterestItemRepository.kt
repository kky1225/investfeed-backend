package com.example.investfeed.domain.interest.repository

import com.example.investfeed.domain.interest.entity.InterestItem
import org.springframework.data.jpa.repository.JpaRepository

interface InterestItemRepository : JpaRepository<InterestItem, Long> {
    fun findByGroupIdOrderByDisplayOrderAsc(groupId: Long): List<InterestItem>
    fun existsByGroupIdAndStkCd(groupId: Long, stkCd: String): Boolean
    fun deleteAllByGroupId(groupId: Long)
    fun countByGroupId(groupId: Long): Int
}
