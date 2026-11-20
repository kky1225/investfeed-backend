package com.example.investfeed.domain.auth.dto.res

import java.time.LocalDateTime

data class MemberRes(
    val id: Long,
    val loginId: String,
    val email: String,
    val nickname: String,
    val name: String,
    val phone: String,
    val role: String,
    val failedLoginAttempts: Int,
    val failedTotpAttempts: Int = 0,
    val lockedAt: LocalDateTime?,
    val lockExpiresAt: LocalDateTime?,
    val permanentLock: Boolean = false,
    val totpEnabled: Boolean = false,
    val secondaryPasswordEnabled: Boolean = false,
    val failedSecondaryAttempts: Int = 0,               // 2차 비밀번호를 연달아 틀린 횟수
    val secondaryPasswordSuspended: Boolean = false,    // 여러 번 틀려 2차 비밀번호가 정지됐는지 (관리자가 초기화해야 풀림)
    val apiKeyLocked: Boolean = false,
    val createdAt: LocalDateTime,
    val permissions: List<UserPermissionRes> = emptyList()
)
