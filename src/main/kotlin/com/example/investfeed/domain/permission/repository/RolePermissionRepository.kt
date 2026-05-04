package com.example.investfeed.domain.permission.repository

import com.example.investfeed.domain.permission.entity.RolePermission
import org.springframework.data.jpa.repository.JpaRepository

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {
    fun existsByRoleIdAndPermissionIdAndAction(roleId: Long, permissionId: Long, action: String): Boolean
    fun findAllByRoleId(roleId: Long): List<RolePermission>
    fun findAllByRoleIdAndPermissionId(roleId: Long, permissionId: Long): List<RolePermission>
}
