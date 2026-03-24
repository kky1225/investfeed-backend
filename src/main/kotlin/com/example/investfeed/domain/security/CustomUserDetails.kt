package com.example.investfeed.domain.security

import com.example.investfeed.domain.auth.entity.Member
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

class CustomUserDetails(val member: Member) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${member.role.name}"))

    override fun getPassword(): String = member.password

    override fun getUsername(): String = member.loginId

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean {
        val expiresAt = member.lockExpiresAt ?: return false // 영구 잠금
        return expiresAt.isBefore(LocalDateTime.now()) // 만료되었으면 잠금 해제
    }

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
