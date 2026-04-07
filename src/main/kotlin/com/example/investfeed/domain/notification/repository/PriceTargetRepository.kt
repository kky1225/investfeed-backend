package com.example.investfeed.domain.notification.repository

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.PriceTarget
import org.springframework.data.jpa.repository.JpaRepository

interface PriceTargetRepository : JpaRepository<PriceTarget, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<PriceTarget>
    fun findByAssetType(assetType: AssetType): List<PriceTarget>
    fun countByMemberId(memberId: Long): Int
}
