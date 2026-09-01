package com.example.investfeed.domain.auth.entity

import com.example.investfeed.common.util.AesEncryptor
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
    var email: String,

    @Column(nullable = false)
    var nickname: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var phone: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    var role: Role,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var passwordChangedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var failedLoginAttempts: Int = 0,

    var lockedAt: LocalDateTime? = null,

    var lockExpiresAt: LocalDateTime? = null,

    @Convert(converter = AesEncryptor::class)
    var totpSecret: String? = null,

    @Column(nullable = false)
    var failedTotpAttempts: Int = 0,

    var secondaryPassword: String? = null,

    @Column(nullable = false)
    var failedApiKeyAttempts: Int = 0,

    @Column(nullable = false)
    var apiKeyLocked: Boolean = false
)
