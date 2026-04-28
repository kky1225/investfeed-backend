package com.example.investfeed.domain.rebalancing.scheduler

import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.entity.NotificationType
import com.example.investfeed.domain.notification.entity.Notification
import com.example.investfeed.domain.notification.repository.NotificationRepository
import com.example.investfeed.domain.notification.config.NotificationWebSocketHandler
import com.example.investfeed.domain.notification.dto.res.NotificationRes
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.monitoring.enum.SchedulerName
import com.example.investfeed.domain.monitoring.service.SchedulerLogService
import com.example.investfeed.domain.rebalancing.repository.RebalancingSettingRepository
import com.example.investfeed.domain.rebalancing.service.RebalancingService
import com.example.investfeed.domain.security.CustomUserDetails
import com.fasterxml.jackson.databind.ObjectMapper
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class RebalancingAlertScheduler(
    private val rebalancingSettingRepository: RebalancingSettingRepository,
    private val rebalancingService: RebalancingService,
    private val memberRepository: MemberRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationWebSocketHandler: NotificationWebSocketHandler,
    private val notificationSettingService: com.example.investfeed.domain.notification.service.NotificationSettingService,
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val schedulerLogService: SchedulerLogService,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 * * * *", scheduler = "slowScheduler")
    fun checkRebalancing() {
        schedulerLogService.execute(SchedulerName.RebalancingAlertScheduler) {
            setSchedulerSecurityContext()
            try {
                authClient.accessToken()
            } catch (e: Exception) {
                log.error(e) { "리밸런싱 스케줄러 토큰 발급 실패" }
                SecurityContextHolder.clearContext()
                return@execute
            }

            val start = System.currentTimeMillis()

            try {
            val allSettings = rebalancingSettingRepository.findAll()

            for (setting in allSettings) {
                try {
                    val member = memberRepository.findById(setting.memberId).orElse(null) ?: continue

                    if (memberApiKeyRepository.findByMemberLoginId(member.loginId).isEmpty()) continue

                    val userDetails = CustomUserDetails(member)
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

                    // 리밸런싱 알림 설정 체크
                    val notiSetting = notificationSettingService.getSettingByMemberId(setting.memberId)
                    if (!notiSetting.rebalancingEnabled) continue

                    val status = rebalancingService.calculateStatus(setting)

                    // 자산 유형 비중 초과 알림
                    for (overweight in status.overweightAssets) {
                        val assetName = when (overweight.assetType) {
                            "STOCK" -> "주식"
                            "CRYPTO" -> "코인"
                            "CASH" -> "현금"
                            else -> overweight.assetType
                        }
                        val code = "REBALANCING_${overweight.assetType}"

                        val notification = notificationRepository.save(Notification(
                            memberId = setting.memberId,
                            type = NotificationType.REBALANCING,
                            assetType = AssetType.TOTAL,
                            assetCode = code,
                            assetName = if (overweight.direction == "MIN") "$assetName 비중 미달" else "$assetName 비중 초과",
                            threshold = overweight.targetRatio.toDouble(),
                            direction = Direction.REBALANCING_ASSET,
                            fluRt = overweight.currentRatio
                        ))

                        sendWebSocket(setting.memberId, notification)
                    }

                    // 종목 비중 초과 알림
                    for (overweight in status.overweightStocks) {
                        val notification = notificationRepository.save(Notification(
                            memberId = setting.memberId,
                            type = NotificationType.REBALANCING,
                            assetType = AssetType.TOTAL,
                            assetCode = overweight.stkCd,
                            assetName = overweight.stkNm,
                            threshold = setting.maxStockRatio.toDouble(),
                            direction = Direction.REBALANCING_STOCK,
                            fluRt = overweight.currentRatio
                        ))

                        sendWebSocket(setting.memberId, notification)
                    }
                } catch (e: Exception) {
                    log.warn { "리밸런싱 체크 실패 (memberId=${setting.memberId}): ${e.message}" }
                }
            }
            } catch (e: Exception) {
                log.error { "RebalancingAlertScheduler 실행 실패: ${e.message}" }
                throw e
            } finally {
                SecurityContextHolder.clearContext()
                log.info { "RebalancingAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
            }
        }
    }

    private fun sendWebSocket(memberId: Long, notification: Notification) {
        try {
            val res = NotificationRes.from(notification)
            val message = objectMapper.writeValueAsString(res)
            notificationWebSocketHandler.sendToUser(memberId, message)
        } catch (e: Exception) {
            log.warn { "리밸런싱 WebSocket 알림 전송 실패: ${e.message}" }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
