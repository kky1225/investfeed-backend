package com.example.investfeed.domain.realizedpnl.entity

import com.example.investfeed.domain.holding.entity.Broker
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "member_realized_pnl",
    uniqueConstraints = [UniqueConstraint(columnNames = ["member_id", "broker_id", "year", "month"])]
)
class MemberRealizedPnl(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker,

    @Column(nullable = false)
    val year: Int,

    @Column(nullable = false)
    val month: Int,

    @Column(name = "realized_pnl", nullable = false)
    var realizedPnl: Long = 0,

    @Column(name = "total_buy_amt")
    var totalBuyAmt: Long? = null,

    @Column(name = "total_sell_amt")
    var totalSellAmt: Long? = null,

    @Column(name = "trade_fee")
    var tradeFee: Long? = null,

    @Column(name = "trade_tax")
    var tradeTax: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var source: PnlSource,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
