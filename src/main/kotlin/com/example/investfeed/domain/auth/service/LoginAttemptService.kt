package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LoginAttemptService(
    private val memberRepository: MemberRepository
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val LOCK_STAGE_1_ATTEMPTS = 5
        const val LOCK_STAGE_2_ATTEMPTS = 10
        const val LOCK_STAGE_3_ATTEMPTS = 20
        const val LOCK_STAGE_1_MINUTES = 10L
        const val LOCK_STAGE_2_MINUTES = 30L
        const val LOCK_STAGE_3_MINUTES = 60L
    }

    data class LoginAttemptResult(
        val lockDurationSeconds: Long? // null이면 잠기지 않음, 양수면 잠금 시간(초)
    )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleFailedLogin(loginId: String): LoginAttemptResult {
        val member = memberRepository.findByLoginId(loginId)
            .orElse(null) ?: return LoginAttemptResult(null)

        member.failedLoginAttempts++

        val lockMinutes = getLockDurationMinutes(member.failedLoginAttempts)

        if (lockMinutes != null) {
            val now = LocalDateTime.now()
            member.lockedAt = now
            member.lockExpiresAt = now.plusMinutes(lockMinutes)
            log.warn { "계정 잠금: loginId=${member.loginId}, 실패횟수=${member.failedLoginAttempts}, 해제시각=${member.lockExpiresAt}" }
        }

        return LoginAttemptResult(lockDurationSeconds = lockMinutes?.let { it * 60 })
    }

    // 아이디만 알면 누구나 실패를 쌓을 수 있으므로 자동 잠금은 항상 시간이 지나면 풀린다. 영구 정지는 관리자만 할 수 있다
    private fun getLockDurationMinutes(attempts: Int): Long? = when {
        attempts >= LOCK_STAGE_3_ATTEMPTS -> LOCK_STAGE_3_MINUTES
        attempts >= LOCK_STAGE_2_ATTEMPTS -> LOCK_STAGE_2_MINUTES
        attempts >= LOCK_STAGE_1_ATTEMPTS -> LOCK_STAGE_1_MINUTES
        else -> null
    }
}
