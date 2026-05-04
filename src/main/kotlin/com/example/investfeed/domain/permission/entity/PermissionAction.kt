package com.example.investfeed.domain.permission.entity

import jakarta.persistence.*

@Entity
@Table(
    name = "permission_actions",
    uniqueConstraints = [UniqueConstraint(name = "permission_actions_unique", columnNames = ["permission_id", "action"])]
)
class PermissionAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    val permission: Permission,

    @Column(nullable = false, length = 20)
    val action: String,

    @Column(length = 255)
    var description: String? = null,
)
