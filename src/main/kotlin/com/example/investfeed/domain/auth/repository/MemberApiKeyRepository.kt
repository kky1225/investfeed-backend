package com.example.investfeed.domain.auth.repository

import com.example.investfeed.domain.auth.entity.MemberApiKey
import org.springframework.data.jpa.repository.JpaRepository

interface MemberApiKeyRepository : JpaRepository<MemberApiKey, Long> {
    fun findByMemberLoginId(loginId: String): List<MemberApiKey>
    fun findByMemberLoginIdAndProvider(loginId: String, provider: String): MemberApiKey?
    fun existsByMemberLoginIdAndProvider(loginId: String, provider: String): Boolean
    fun findAllByProvider(provider: String): List<MemberApiKey>
}
