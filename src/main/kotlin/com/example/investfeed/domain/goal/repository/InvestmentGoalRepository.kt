package com.example.investfeed.domain.goal.repository

import com.example.investfeed.domain.goal.entity.GoalType
import com.example.investfeed.domain.goal.entity.InvestmentGoal
import org.springframework.data.jpa.repository.JpaRepository

interface InvestmentGoalRepository : JpaRepository<InvestmentGoal, Long> {

    fun findByMemberIdOrderByIdAsc(memberId: Long): List<InvestmentGoal>

    fun findByMemberIdAndId(memberId: Long, id: Long): InvestmentGoal?

    fun findByMemberIdAndType(memberId: Long, type: GoalType): InvestmentGoal?
}
