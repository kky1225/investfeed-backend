package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.notification.dto.req.NotificationSettingReq
import com.example.investfeed.domain.notification.dto.res.NotificationSettingRes
import com.example.investfeed.domain.notification.entity.NotificationSetting
import com.example.investfeed.domain.notification.repository.NotificationSettingRepository
import com.example.investfeed.domain.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationSettingService(
    private val notificationSettingRepository: NotificationSettingRepository,
) {

    fun getSetting(): NotificationSettingRes {
        val memberId = getMemberId()
        val setting = notificationSettingRepository.findByMemberId(memberId)
        return if (setting != null) toRes(setting) else defaultSetting()
    }

    fun getSettingByMemberId(memberId: Long): NotificationSetting {
        return notificationSettingRepository.findByMemberId(memberId)
            ?: NotificationSetting(memberId = memberId)
    }

    @Transactional
    fun saveSetting(req: NotificationSettingReq): NotificationSettingRes {
        val memberId = getMemberId()
        val existing = notificationSettingRepository.findByMemberId(memberId)

        val setting = if (existing != null) {
            existing.priceUpEnabled = req.priceUpEnabled
            existing.priceDownEnabled = req.priceDownEnabled
            existing.high52wEnabled = req.high52wEnabled
            existing.low52wEnabled = req.low52wEnabled
            existing.upperLimitEnabled = req.upperLimitEnabled
            existing.lowerLimitEnabled = req.lowerLimitEnabled
            existing.goalEnabled = req.goalEnabled
            existing.rebalancingEnabled = req.rebalancingEnabled
            existing.updatedAt = LocalDateTime.now()
            existing
        } else {
            notificationSettingRepository.save(
                NotificationSetting(
                    memberId = memberId,
                    priceUpEnabled = req.priceUpEnabled,
                    priceDownEnabled = req.priceDownEnabled,
                    high52wEnabled = req.high52wEnabled,
                    low52wEnabled = req.low52wEnabled,
                    upperLimitEnabled = req.upperLimitEnabled,
                    lowerLimitEnabled = req.lowerLimitEnabled,
                    goalEnabled = req.goalEnabled,
                    rebalancingEnabled = req.rebalancingEnabled,
                )
            )
        }

        return toRes(setting)
    }

    private fun toRes(setting: NotificationSetting): NotificationSettingRes {
        return NotificationSettingRes(
            priceUpEnabled = setting.priceUpEnabled,
            priceDownEnabled = setting.priceDownEnabled,
            high52wEnabled = setting.high52wEnabled,
            low52wEnabled = setting.low52wEnabled,
            upperLimitEnabled = setting.upperLimitEnabled,
            lowerLimitEnabled = setting.lowerLimitEnabled,
            goalEnabled = setting.goalEnabled,
            rebalancingEnabled = setting.rebalancingEnabled,
        )
    }

    private fun defaultSetting(): NotificationSettingRes {
        return NotificationSettingRes(
            priceUpEnabled = true, priceDownEnabled = true,
            high52wEnabled = true, low52wEnabled = true,
            upperLimitEnabled = true, lowerLimitEnabled = true,
            goalEnabled = true, rebalancingEnabled = true,
        )
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
