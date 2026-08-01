package com.example.investfeed.domain.us.stock.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "us_stock_master")
class UsStockMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val stexTp: String, // NA:AMEX, ND:NASDAQ, NY:NYSE

    @Column(nullable = false)
    val stkCd: String,

    @Column
    val stkNm: String? = null,

    @Column
    val stkEnm: String? = null, // 종목영문명

    @Column
    val mkgb: String? = null, // 거래소명

    @Column
    val upgb: String? = null, // 업종명

    @Column
    val isEtf: String? = null,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
