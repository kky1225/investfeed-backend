package com.example.investfeed.domain.menu.repository

import com.example.investfeed.domain.menu.entity.MenuBrokerPermission
import org.springframework.data.jpa.repository.JpaRepository

interface MenuBrokerPermissionRepository : JpaRepository<MenuBrokerPermission, Long> {
    fun countByBrokerId(brokerId: Long): Long
}
