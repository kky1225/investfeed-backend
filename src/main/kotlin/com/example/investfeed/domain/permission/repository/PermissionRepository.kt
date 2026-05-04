package com.example.investfeed.domain.permission.repository

import com.example.investfeed.domain.permission.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByCode(code: String): Permission?
    fun findAllByOrderByOrderIndexAsc(): List<Permission>
}
