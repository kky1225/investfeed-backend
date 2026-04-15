package com.example.investfeed.domain.goal.scheduler

import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.goal.entity.GoalType
import com.example.investfeed.domain.goal.repository.InvestmentGoalRepository
import com.example.investfeed.domain.goal.service.InvestmentGoalService
import com.example.investfeed.domain.notification.service.NotificationService
import com.example.investfeed.domain.notification.service.NotificationSettingService
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.kiwoom.auth.service.AuthClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GoalAlertScheduler(
    private val investmentGoalRepository: InvestmentGoalRepository,
    private val investmentGoalService: InvestmentGoalService,
    private val memberRepository: MemberRepository,
    private val notificationService: NotificationService,
    private val notificationSettingService: NotificationSettingService,
    private val authClient: AuthClient,
    @param:Value("\${scheduler.login-id:admin}")
    private val schedulerLoginId: String
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 * * * *", scheduler = "slowScheduler")
    fun checkGoals() {
        setSchedulerSecurityContext()
        try {
            authClient.accessToken()
        } catch (e: Exception) {
            log.error(e) { "목표 스케줄러 토큰 발급 실패" }
            SecurityContextHolder.clearContext()
            return
        }

        val start = System.currentTimeMillis()

        try {
            val allGoals = investmentGoalRepository.findAll()

            val memberGoals = allGoals.groupBy { it.memberId }

            for ((memberId, goals) in memberGoals) {
                // 해당 유저의 SecurityContext 설정
                val member = memberRepository.findById(memberId).orElse(null) ?: continue
                val userDetails = CustomUserDetails(member)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

                // 목표 알림 설정 체크
                val notiSetting = notificationSettingService.getSettingByMemberId(memberId)
                if (!notiSetting.goalEnabled) continue

                for (goal in goals) {
                    try {
                        val currentAmount = investmentGoalService.calculateCurrentAmount(memberId, goal.type)

                        if (currentAmount >= goal.targetAmount && !goal.isAchieved) {
                            goal.isAchieved = true
                            investmentGoalRepository.save(goal)

                            notificationService.createGoalAlert(
                                memberId = memberId,
                                goalType = goal.type,
                                targetAmount = goal.targetAmount,
                                currentAmount = currentAmount
                            )
                        } else if (currentAmount < goal.targetAmount && goal.isAchieved) {
                            val shouldReset = when (goal.type) {
                                GoalType.MONTHLY_REALIZED_PNL -> true
                                GoalType.YEARLY_REALIZED_PNL -> true
                                GoalType.TOTAL_ASSET -> false
                            }
                            if (shouldReset) {
                                goal.isAchieved = false
                                investmentGoalRepository.save(goal)
                            }
                        }
                    } catch (e: Exception) {
                        log.error { "목표 체크 실패 (memberId=$memberId, goalId=${goal.id}): ${e.message}" }
                    }
                }
            }
        } catch (e: Exception) {
            log.error { "GoalAlertScheduler 실행 실패: ${e.message}" }
        } finally {
            SecurityContextHolder.clearContext()
            log.info { "GoalAlertScheduler 실행 완료: ${System.currentTimeMillis() - start}ms" }
        }
    }

    private fun setSchedulerSecurityContext() {
        val auth = UsernamePasswordAuthenticationToken(schedulerLoginId, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }
}
