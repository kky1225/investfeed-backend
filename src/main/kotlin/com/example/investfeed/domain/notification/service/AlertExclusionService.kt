package com.example.investfeed.domain.notification.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class AlertExclusionService(
    private val memberRepository: MemberRepository,
) {
    companion object {
        private const val SUPER_ADMIN_ROLE = "SUPER_ADMIN"
    }

    fun excludedMemberIds(): Set<Long> = memberRepository.findIdsByRoleCode(SUPER_ADMIN_ROLE).toSet()
}
