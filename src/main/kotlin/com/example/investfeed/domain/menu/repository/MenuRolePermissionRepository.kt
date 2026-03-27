package com.example.investfeed.domain.menu.repository

import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.menu.entity.MenuRolePermission
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRolePermissionRepository : JpaRepository<MenuRolePermission, Long> {
    fun findByMenuIdAndRole(menuId: Long, role: Role): MenuRolePermission?
}
