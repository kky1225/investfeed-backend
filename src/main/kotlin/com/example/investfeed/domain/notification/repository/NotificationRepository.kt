package com.example.investfeed.domain.notification.repository

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: Long): List<Notification>
    fun findByMemberIdAndAssetTypeOrderByCreatedAtDesc(memberId: Long, assetType: AssetType): List<Notification>
    fun findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId: Long): List<Notification>
    fun countByMemberIdAndIsReadFalse(memberId: Long): Int
}
