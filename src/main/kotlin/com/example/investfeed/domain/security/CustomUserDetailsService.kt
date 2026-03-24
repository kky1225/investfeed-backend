package com.example.investfeed.domain.security

import com.example.investfeed.domain.auth.exception.MemberNotFoundException
import com.example.investfeed.domain.auth.repository.MemberRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val member = memberRepository.findByLoginId(username)
            .orElseThrow { MemberNotFoundException() }
        return CustomUserDetails(member)
    }
}
