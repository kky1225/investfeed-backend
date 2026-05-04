package com.example.investfeed.domain.permission.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "permission_api_patterns",
    uniqueConstraints = [UniqueConstraint(columnNames = ["api_pattern"])]
)
class PermissionApiPattern(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    val permission: Permission,

    @Column(name = "api_pattern", nullable = false)
    var apiPattern: String,
)
