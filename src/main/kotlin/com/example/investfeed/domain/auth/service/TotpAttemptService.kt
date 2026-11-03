package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.repository.MemberRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TotpAttemptService(
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

    data class TotpAttemptResult(
        val lockDurationSeconds: Long?, // null이면 잠기지 않음, 양수면 잠금 시간(초)
        val remainingAttempts: Int?     // 다음 잠금까지 남은 횟수, 잠금 발동 시 null
    )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleFailedTotp(loginId: String): TotpAttemptResult {
        val member = memberRepository.findByLoginId(loginId)
            .orElse(null) ?: return TotpAttemptResult(null, null)

        member.failedTotpAttempts++

        val lockMinutes = getLockDurationMinutes(member.failedTotpAttempts)

        if (lockMinutes != null) {
            val now = LocalDateTime.now()
            member.lockedAt = now
            member.lockExpiresAt = now.plusMinutes(lockMinutes)
            log.warn { "TOTP 실패 계정 잠금: loginId=${member.loginId}, 실패횟수=${member.failedTotpAttempts}, 해제시각=${member.lockExpiresAt}" }

            return TotpAttemptResult(
                lockDurationSeconds = lockMinutes * 60,
                remainingAttempts = null
            )
        }

        return TotpAttemptResult(
            lockDurationSeconds = null,
            remainingAttempts = remainingUntilNextLock(member.failedTotpAttempts)
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resetOnSuccess(loginId: String) {
        val member = memberRepository.findByLoginId(loginId).orElse(null) ?: return
        if (member.failedTotpAttempts > 0) {
            member.failedTotpAttempts = 0
        }
    }

    // 자동 잠금은 항상 시간이 지나면 풀린다. 영구 정지는 관리자만 할 수 있다
    private fun getLockDurationMinutes(attempts: Int): Long? = when {
        attempts >= LOCK_STAGE_3_ATTEMPTS -> LOCK_STAGE_3_MINUTES
        attempts >= LOCK_STAGE_2_ATTEMPTS -> LOCK_STAGE_2_MINUTES
        attempts >= LOCK_STAGE_1_ATTEMPTS -> LOCK_STAGE_1_MINUTES
        else -> null
    }

    private fun remainingUntilNextLock(attempts: Int): Int? {
        val next = listOf(LOCK_STAGE_1_ATTEMPTS, LOCK_STAGE_2_ATTEMPTS, LOCK_STAGE_3_ATTEMPTS)
            .firstOrNull { it > attempts } ?: return null
        return next - attempts
    }
}
