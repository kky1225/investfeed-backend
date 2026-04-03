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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker,

    @Column(name = "pur_price")
    var purPrice: Long? = null,

    var quantity: Long? = null,

    @Column(name = "pur_amt")
    var purAmt: Long? = null,

    @Column(name = "display_order")
    var displayOrder: Int = 0,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
