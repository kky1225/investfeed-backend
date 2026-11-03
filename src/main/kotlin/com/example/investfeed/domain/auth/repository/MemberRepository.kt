package com.example.investfeed.domain.auth.repository

import com.example.investfeed.domain.auth.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByLoginId(loginId: String): Optional<Member>
    fun existsByLoginId(loginId: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
    fun existsByPhone(phone: String): Boolean
    fun existsByEmailAndLoginIdNot(email: String, loginId: String): Boolean
    fun existsByPhoneAndLoginIdNot(phone: String, loginId: String): Boolean
    fun existsByRoleId(roleId: Long): Boolean

    @Query("SELECT m.id FROM Member m WHERE m.role.code = :roleCode")
    fun findIdsByRoleCode(roleCode: String): List<Long>
}
