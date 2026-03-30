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
    val lockedAt: LocalDateTime?,
    val lockExpiresAt: LocalDateTime?,
    val permanentLock: Boolean = false,
    val totpEnabled: Boolean = false,
    val secondaryPasswordEnabled: Boolean = false,
    val createdAt: LocalDateTime
)
