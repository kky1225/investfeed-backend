package com.example.investfeed.domain.menu.entity

import com.example.investfeed.domain.permission.entity.Permission
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "menus")
class Menu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    var url: String? = null,

    var icon: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Menu? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_permission_id")
    var requiredPermission: Permission? = null,

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0,

    @Column(nullable = false)
    var visible: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "parent")
    @OrderBy("orderIndex ASC")
    val children: MutableList<Menu> = mutableListOf(),

    @OneToMany(mappedBy = "menu", cascade = [CascadeType.ALL], orphanRemoval = true)
    val brokerPermissions: MutableList<MenuBrokerPermission> = mutableListOf()
)
