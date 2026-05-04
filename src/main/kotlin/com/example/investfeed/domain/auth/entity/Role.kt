package com.example.investfeed.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val code: String,

    @Column(nullable = false)
    var name: String,

    @Column(name = "default_landing_path")
    var defaultLandingPath: String? = null,

    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false,

    @Column(nullable = false)
    var priority: Int,

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
