package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock_avoid")
class StockAvoid(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    val stkNm: String,

    val fluRt: String? = null,

    val curPrc: String? = null,

    val preSig: String? = null
)
