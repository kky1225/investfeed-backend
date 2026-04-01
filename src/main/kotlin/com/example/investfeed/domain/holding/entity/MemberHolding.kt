package com.example.investfeed.domain.holding.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "member_holdings")
class MemberHolding(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "stk_cd", nullable = false)
    var stkCd: String,

    @Column(name = "stk_nm", nullable = false)
    var stkNm: String,

    @Column(nullable = false)
    var provider: String,

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
