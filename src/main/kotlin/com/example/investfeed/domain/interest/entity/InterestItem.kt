package com.example.investfeed.domain.interest.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interest_item")
class InterestItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val groupId: Long,

    @Column(nullable = false)
    val stkCd: String,

    @Column(nullable = false)
    val stkNm: String,

    @Column(nullable = false)
    val addedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false, columnDefinition = "integer default 0")
    var displayOrder: Int = 0
)
