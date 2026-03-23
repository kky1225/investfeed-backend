package com.example.investfeed.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val loginId: String,

    @Column(nullable = false)
    var password: String,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val phone: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var passwordChangedAt: LocalDateTime = LocalDateTime.now()
)

enum class Role {
    USER, ADMIN
}
