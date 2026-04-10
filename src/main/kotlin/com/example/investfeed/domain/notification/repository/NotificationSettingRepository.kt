package com.example.investfeed.domain.notification.repository

import com.example.investfeed.domain.notification.entity.NotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {

    fun findByMemberId(memberId: Long): NotificationSetting?
}
