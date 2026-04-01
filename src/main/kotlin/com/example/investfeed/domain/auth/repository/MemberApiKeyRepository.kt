package com.example.investfeed.domain.auth.repository

import com.example.investfeed.domain.auth.entity.MemberApiKey
import org.springframework.data.jpa.repository.JpaRepository

interface MemberApiKeyRepository : JpaRepository<MemberApiKey, Long> {
    fun findByMemberLoginId(loginId: String): List<MemberApiKey>
    fun findByMemberLoginIdAndBrokerId(loginId: String, brokerId: Long): MemberApiKey?
    fun existsByMemberLoginIdAndBrokerId(loginId: String, brokerId: Long): Boolean
    fun findAllByBrokerId(brokerId: Long): List<MemberApiKey>
}
