package com.example.investfeed.domain.auth.entity

import com.example.investfeed.common.util.AesEncryptor
import com.example.investfeed.domain.holding.entity.Broker
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "member_api_keys")
class MemberApiKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker,

    @Convert(converter = AesEncryptor::class)
    @Column(nullable = false)
    var appKey: String,

    @Convert(converter = AesEncryptor::class)
    @Column(nullable = false)
    var secretKey: String,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
