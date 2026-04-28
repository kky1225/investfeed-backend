package com.example.investfeed.domain.menu.entity

import com.example.investfeed.domain.holding.entity.Broker
import jakarta.persistence.*

@Entity
@Table(
    name = "menu_broker_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["menu_id", "broker_id"])]
)
class MenuBrokerPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    val menu: Menu,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id", nullable = false)
    val broker: Broker
)
