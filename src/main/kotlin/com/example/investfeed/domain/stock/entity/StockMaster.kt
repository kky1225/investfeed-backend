package com.example.investfeed.domain.stock.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stock_master")
class StockMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    val stkNm: String,

    @Column(nullable = false)
    val mrktTp: String, // 0:코스피, 10:코스닥, 8:ETF, 60:ETN (첫 등장 기준)

    @Column
    val mrktNm: String? = null,

    @Column
    val upNm: String? = null, // 업종명

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
