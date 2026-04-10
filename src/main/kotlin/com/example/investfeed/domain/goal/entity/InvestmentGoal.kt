package com.example.investfeed.domain.goal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "investment_goals")
class InvestmentGoal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: GoalType,

    @Column(name = "target_amount", nullable = false)
    var targetAmount: Long,

    @Column(name = "is_achieved", nullable = false)
    var isAchieved: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
