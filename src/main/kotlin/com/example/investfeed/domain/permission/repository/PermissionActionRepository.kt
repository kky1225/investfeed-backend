package com.example.investfeed.domain.permission.repository

import com.example.investfeed.domain.permission.entity.PermissionAction
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionActionRepository : JpaRepository<PermissionAction, Long> {
    fun findAllByPermissionId(permissionId: Long): List<PermissionAction>
    fun findByPermissionIdAndAction(permissionId: Long, action: String): PermissionAction?
    fun existsByPermissionIdAndAction(permissionId: Long, action: String): Boolean
}
