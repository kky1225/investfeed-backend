package com.example.investfeed.domain.permission.entity

import com.example.investfeed.domain.auth.entity.Role
import jakarta.persistence.*

@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = [UniqueConstraint(name = "role_permissions_unique", columnNames = ["role_id", "permission_id", "action"])]
)
class RolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    val role: Role,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    val permission: Permission,

    @Column(nullable = false, length = 20)
    val action: String,
)
