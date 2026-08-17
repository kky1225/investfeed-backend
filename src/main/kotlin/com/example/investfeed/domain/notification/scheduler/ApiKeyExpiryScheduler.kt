package com.example.investfeed.domain.notification.scheduler

import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerCron
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.notification.service.NotificationSettingService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class ApiKeyExpiryScheduler(
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val notificationService: NotificationService,
    private val notificationSettingService: NotificationSettingService,
    private val schedulerLogService: SchedulerLogService,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = SchedulerCron.API_KEY_EXPIRY, scheduler = "slowScheduler")
    fun checkApiKeyExpiry() {
        schedulerLogService.execute(SchedulerName.ApiKeyExpiryScheduler) {
            val start = System.currentTimeMillis()
            val today = LocalDate.now()
            val allApiKeys = memberApiKeyRepository.findAll()

            for (apiKey in allApiKeys) {
                try {
                    // 알림 설정 체크
                    val notiSetting = notificationSettingService.getSettingByMemberId(apiKey.member.id)
                    if (!notiSetting.apiKeyEnabled) continue

                    val expiresDate = apiKey.expiresAt.toLocalDate()
                    val daysLeft = ChronoUnit.DAYS.between(today, expiresDate).toInt()

                    val direction = when (daysLeft) {
                        30 -> Direction.API_KEY_EXPIRY_30D
                        7 -> Direction.API_KEY_EXPIRY_7D
                        0 -> Direction.API_KEY_EXPIRED
                        else -> continue
                    }

                    notificationService.createApiKeyExpiryAlert(
                        memberId = apiKey.member.id,
                        apiKeyId = apiKey.id,
                        brokerName = apiKey.broker.name,
                        direction = direction,
                        daysLeft = daysLeft
                    )
                } catch (e: Exception) {
                    log.warn { "API Key 만료 체크 실패 (apiKeyId=${apiKey.id}): ${e.message}" }
                }
            }
            log.info { "ApiKeyExpiryScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }
}
