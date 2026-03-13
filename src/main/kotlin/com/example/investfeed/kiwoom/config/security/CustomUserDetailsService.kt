package com.example.investfeed.kiwoom.config.security

import com.example.investfeed.domain.auth.repository.MemberRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("존재하지 않는 이메일입니다.") }
        return CustomUserDetails(member)
    }
}
