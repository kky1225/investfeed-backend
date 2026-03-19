package com.example.investfeed.domain.cryptointerest.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "crypto_interest_group")
class CryptoInterestGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val memberId: Long,

    @Column(nullable = false)
    var groupNm: String,

    @Column(nullable = false)
    var displayOrder: Int = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
