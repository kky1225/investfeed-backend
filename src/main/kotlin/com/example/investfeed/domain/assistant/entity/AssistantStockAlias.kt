package com.example.investfeed.domain.assistant.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "assistant_stock_alias")
class AssistantStockAlias(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 50, unique = true)
    val alias: String,

    @Column(nullable = false, length = 10)
    val market: String,   // KR / US / CRYPTO

    @Column(name = "stk_cd", nullable = false, length = 20)
    val stkCd: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
