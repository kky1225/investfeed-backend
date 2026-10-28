package com.example.investfeed.domain.watchlist.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stock_watchlist_item")
class WatchlistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val groupId: Long,

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    var stkNm: String,

    @Column
    val stexTp: String? = null, // NULL: 국내, ND/NY/NA: 미국 거래소

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, columnDefinition = "integer default 0")
    var displayOrder: Int = 0
)
