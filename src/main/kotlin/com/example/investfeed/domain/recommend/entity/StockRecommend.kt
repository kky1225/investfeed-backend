package com.example.investfeed.domain.recommend.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock_recommend")
class StockRecommend(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    val stkNm: String,
)
