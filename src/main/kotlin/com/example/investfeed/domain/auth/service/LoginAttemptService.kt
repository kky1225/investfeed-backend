package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.entity.Role
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
        val locked: Boolean,
        val lockDurationSeconds: Long? // null이면 영구잠금, 양수면 잠금 시간(초)
    )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleFailedLogin(loginId: String): LoginAttemptResult {
        val member = memberRepository.findByLoginId(loginId)
            .orElse(null) ?: return LoginAttemptResult(false, null)

        member.failedLoginAttempts++

        val lockMinutes = getLockDurationMinutes(member.failedLoginAttempts, member.role)

        if (lockMinutes != null) {
            val now = LocalDateTime.now()
            member.lockedAt = now
            member.lockExpiresAt = if (lockMinutes < 0) null else now.plusMinutes(lockMinutes)
            log.warn { "계정 잠금: loginId=${member.loginId}, 실패횟수=${member.failedLoginAttempts}, 해제시각=${member.lockExpiresAt ?: "영구"}" }
        }

        return LoginAttemptResult(
            locked = lockMinutes != null,
            lockDurationSeconds = lockMinutes?.let { if (it < 0) null else it * 60 }
        )
    }

    private fun getLockDurationMinutes(attempts: Int, role: Role): Long? {
        if (role == Role.ADMIN) {
            return when {
                attempts >= LOCK_STAGE_3_ATTEMPTS -> LOCK_STAGE_3_MINUTES
                attempts >= LOCK_STAGE_2_ATTEMPTS -> LOCK_STAGE_2_MINUTES
                attempts >= LOCK_STAGE_1_ATTEMPTS -> LOCK_STAGE_1_MINUTES
                else -> null
            }
        }

        return when {
            attempts >= LOCK_STAGE_3_ATTEMPTS -> -1L // 영구 잠금
            attempts >= LOCK_STAGE_2_ATTEMPTS -> LOCK_STAGE_2_MINUTES
            attempts >= LOCK_STAGE_1_ATTEMPTS -> LOCK_STAGE_1_MINUTES
            else -> null
        }
    }
}
