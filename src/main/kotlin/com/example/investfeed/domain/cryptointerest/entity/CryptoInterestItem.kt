package com.example.investfeed.domain.cryptointerest.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "crypto_interest_item")
class CryptoInterestItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val groupId: Long,

    @Column(nullable = false)
    val market: String,

    @Column(nullable = false)
    val koreanName: String,

    @Column(nullable = false)
    val addedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, columnDefinition = "integer default 0")
    var displayOrder: Int = 0
)
