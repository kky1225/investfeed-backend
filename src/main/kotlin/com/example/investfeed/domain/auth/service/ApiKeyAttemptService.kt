package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ApiKeyAttemptService(
    private val memberRepository: MemberRepository
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val MAX_ATTEMPTS = 5
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleFailedRegistration(loginId: String) {
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return

        member.failedApiKeyAttempts++

        if (member.failedApiKeyAttempts >= MAX_ATTEMPTS && !member.apiKeyLocked) {
            member.apiKeyLocked = true
            log.warn { "API key 등록 영구 잠금: loginId=$loginId, attempts=${member.failedApiKeyAttempts}" }
        }
    }

    @Transactional
    fun resetOnSuccess(loginId: String) {
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return
        if (member.failedApiKeyAttempts > 0) {
            member.failedApiKeyAttempts = 0
        }
    }

    @Transactional
    fun unlock(loginId: String) {
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return
        member.apiKeyLocked = false
        member.failedApiKeyAttempts = 0
        log.info { "API key 등록 잠금 해제: loginId=$loginId" }
    }
}
