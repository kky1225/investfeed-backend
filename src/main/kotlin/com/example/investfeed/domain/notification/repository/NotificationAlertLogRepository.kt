package com.example.investfeed.domain.notification.repository

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.entity.NotificationAlertLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface NotificationAlertLogRepository : JpaRepository<NotificationAlertLog, Long> {
    fun existsByMemberIdAndAssetTypeAndAssetCodeAndThresholdAndDirectionAndAlertDate(
        memberId: Long,
        assetType: AssetType,
        assetCode: String,
        threshold: Double,
        direction: Direction,
        alertDate: LocalDate
    ): Boolean

    fun deleteByMemberIdAndAssetTypeAndAssetCodeAndDirection(
        memberId: Long,
        assetType: AssetType,
        assetCode: String,
        direction: Direction
    )
}
