package com.example.investfeed.domain.auth.repository

import com.example.investfeed.domain.auth.entity.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByCode(code: String): Role?
    fun findAllByOrderByPriorityAsc(): List<Role>
}
