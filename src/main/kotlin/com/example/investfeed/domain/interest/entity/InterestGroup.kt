package com.example.investfeed.domain.interest.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "interest_group")
class InterestGroup(
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
