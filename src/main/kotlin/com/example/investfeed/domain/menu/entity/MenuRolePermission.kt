package com.example.investfeed.domain.menu.entity

import com.example.investfeed.domain.auth.entity.Role
import jakarta.persistence.*

@Entity
@Table(
    name = "menu_role_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["menu_id", "role"])]
)
class MenuRolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,

    @Column(nullable = false)
    var readable: Boolean = true
)
