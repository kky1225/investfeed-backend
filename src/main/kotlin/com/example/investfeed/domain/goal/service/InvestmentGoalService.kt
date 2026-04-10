package com.example.investfeed.domain.goal.service

import com.example.investfeed.domain.goal.dto.req.InvestmentGoalCreateReq
import com.example.investfeed.domain.goal.dto.req.InvestmentGoalUpdateReq
import com.example.investfeed.domain.goal.dto.res.GoalDashboardRes
import com.example.investfeed.domain.goal.dto.res.InvestmentGoalRes
import com.example.investfeed.domain.goal.entity.GoalType
import com.example.investfeed.domain.goal.entity.InvestmentGoal
import com.example.investfeed.domain.goal.repository.InvestmentGoalRepository
import com.example.investfeed.domain.holding.service.AssetDashboardService
import com.example.investfeed.domain.notification.entity.AssetType
import com.example.investfeed.domain.notification.entity.Direction
import com.example.investfeed.domain.notification.repository.NotificationAlertLogRepository
import com.example.investfeed.domain.realizedpnl.dto.req.RealizedPnlSyncReq
import com.example.investfeed.domain.realizedpnl.repository.MemberRealizedPnlRepository
import com.example.investfeed.domain.realizedpnl.service.StockRealizedPnlService
import com.example.investfeed.domain.security.CustomUserDetails
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class InvestmentGoalService(
    private val investmentGoalRepository: InvestmentGoalRepository,
    private val assetDashboardService: AssetDashboardService,
    private val stockRealizedPnlService: StockRealizedPnlService,
    private val memberRealizedPnlRepository: MemberRealizedPnlRepository,
    private val alertLogRepository: NotificationAlertLogRepository,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun create(req: InvestmentGoalCreateReq): InvestmentGoalRes {
        val memberId = getMemberId()

        val existing = investmentGoalRepository.findByMemberIdAndType(memberId, req.type)
        if (existing != null) {
            throw IllegalArgumentException("이미 해당 유형의 목표가 존재합니다.")
        }

        val goal = investmentGoalRepository.save(
            InvestmentGoal(
                memberId = memberId,
                type = req.type,
                targetAmount = req.targetAmount,
            )
        )

        return toRes(goal, calculateCurrentAmount(memberId, goal.type))
    }

    @Transactional
    fun update(id: Long, req: InvestmentGoalUpdateReq): InvestmentGoalRes {
        val memberId = getMemberId()
        val goal = investmentGoalRepository.findByMemberIdAndId(memberId, id)
            ?: throw IllegalArgumentException("목표를 찾을 수 없습니다.")

        goal.targetAmount = req.targetAmount
        goal.isAchieved = false
        goal.updatedAt = LocalDateTime.now()

        // 목표 변경 시 기존 알림 로그 삭제 (재알림 가능하도록)
        alertLogRepository.deleteByMemberIdAndAssetTypeAndAssetCodeAndDirection(
            memberId, AssetType.TOTAL, goal.type.name, Direction.GOAL_ACHIEVED
        )

        return toRes(goal, calculateCurrentAmount(memberId, goal.type))
    }

    @Transactional
    fun delete(id: Long) {
        val memberId = getMemberId()
        val goal = investmentGoalRepository.findByMemberIdAndId(memberId, id)
            ?: throw IllegalArgumentException("목표를 찾을 수 없습니다.")

        investmentGoalRepository.delete(goal)
    }

    fun getGoals(): GoalDashboardRes {
        val memberId = getMemberId()
        val goals = investmentGoalRepository.findByMemberId(memberId)

        return GoalDashboardRes(
            goals = goals.map { goal ->
                toRes(goal, calculateCurrentAmount(memberId, goal.type))
            }
        )
    }

    fun getDashboardGoals(): GoalDashboardRes {
        return getGoals()
    }

    fun calculateCurrentAmount(memberId: Long, type: GoalType): Long {
        return try {
            when (type) {
                GoalType.TOTAL_ASSET -> {
                    val dashboard = assetDashboardService.dashboard()
                    dashboard.totalAsset
                }
                GoalType.MONTHLY_REALIZED_PNL -> {
                    val now = LocalDate.now()
                    val apiPnl = stockRealizedPnlService.fetchApiPnl(
                        RealizedPnlSyncReq(year = now.year, month = now.monthValue)
                    ).totalRealizedPnl
                    val manualPnl = memberRealizedPnlRepository.findByMemberId(memberId)
                        .filter { it.year == now.year && it.month == now.monthValue }
                        .sumOf { it.realizedPnl }
                    apiPnl + manualPnl
                }
                GoalType.YEARLY_REALIZED_PNL -> {
                    val now = LocalDate.now()
                    val apiPnl = stockRealizedPnlService.fetchApiPnl(
                        RealizedPnlSyncReq(year = now.year)
                    ).totalRealizedPnl
                    val manualPnl = memberRealizedPnlRepository.findByMemberId(memberId)
                        .filter { it.year == now.year }
                        .sumOf { it.realizedPnl }
                    apiPnl + manualPnl
                }
            }
        } catch (e: Exception) {
            log.error { "목표 달성률 계산 실패: ${e.message}" }
            0L
        }
    }

    private fun toRes(goal: InvestmentGoal, currentAmount: Long): InvestmentGoalRes {
        val rate = if (goal.targetAmount > 0) {
            (currentAmount.toDouble() / goal.targetAmount * 100)
        } else 0.0

        return InvestmentGoalRes(
            id = goal.id,
            type = goal.type.name,
            targetAmount = goal.targetAmount,
            currentAmount = currentAmount,
            achievementRate = Math.round(rate * 100) / 100.0,
            isAchieved = goal.isAchieved,
            createdAt = goal.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        )
    }

    private fun getMemberId(): Long {
        val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails
        return userDetails?.member?.id ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
    }
}
