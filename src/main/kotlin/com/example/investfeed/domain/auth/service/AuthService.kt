package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.ApiKeyReq
import com.example.investfeed.domain.auth.dto.req.ChangePasswordReq
import com.example.investfeed.domain.auth.dto.req.CreateMemberReq
import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.req.UpdateProfileReq
import com.example.investfeed.domain.auth.dto.res.ApiKeyRes
import com.example.investfeed.domain.auth.dto.res.MemberRes
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.entity.MemberApiKey
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.security.JwtProvider
import com.example.investfeed.domain.auth.exception.*
import org.springframework.security.access.AccessDeniedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service("memberAuthService")
class AuthService(
    @param:Value("\${security.password-change-cycle}")
    private val passwordChangeCycle: Long,
    @param:Value("\${security.default-password}")
    private val defaultPassword: String,
    private val memberRepository: MemberRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val loginAttemptService: LoginAttemptService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun signup(
        req: SignupReq
    ) {
        if (memberRepository.existsByLoginId(req.loginId)) {
            throw DuplicateLoginIdException()
        }
        if (memberRepository.existsByEmail(req.email)) {
            throw DuplicateEmailException()
        }
        if (memberRepository.existsByNickname(req.nickname)) {
            throw DuplicateNicknameException()
        }
        if (memberRepository.existsByPhone(req.phone)) {
            throw DuplicatePhoneException()
        }

        val member = Member(
            loginId = req.loginId,
            password = passwordEncoder.encode(req.password),
            email = req.email,
            nickname = req.nickname,
            name = req.name,
            phone = req.phone
        )

        memberRepository.save(member)
    }

    data class LoginResult(
        val tokenRes: TokenRes,
        val accessToken: String,
        val refreshToken: String
    )

    @Transactional
    fun login(req: LoginReq): LoginResult {
        val member = memberRepository.findByLoginId(req.loginId)
            .orElseThrow { InvalidCredentialsException() }

        checkAccountLock(member)

        if (!passwordEncoder.matches(req.password, member.password)) {
            val locked = loginAttemptService.handleFailedLogin(member.loginId)
            if (locked) {
                throw AccountLockedByFailureException()
            }
            throw InvalidCredentialsException()
        }

        member.failedLoginAttempts = 0
        member.lockedAt = null
        member.lockExpiresAt = null

        val passwordChangeRequired = member.passwordChangedAt
            .plusDays(passwordChangeCycle)
            .isBefore(LocalDateTime.now())

        return LoginResult(
            tokenRes = TokenRes(
                passwordChangeRequired = passwordChangeRequired,
                role = member.role.name,
                nickname = member.nickname,
                email = maskEmail(member.email)
            ),
            accessToken = jwtProvider.generateAccessToken(member.loginId),
            refreshToken = jwtProvider.generateRefreshToken(member.loginId)
        )
    }

    private fun checkAccountLock(member: Member) {
        member.lockedAt ?: return

        if (member.lockExpiresAt == null) {
            throw AccountPermanentlyLockedException()
        }

        if (member.lockExpiresAt!!.isAfter(LocalDateTime.now())) {
            val remainingMinutes = java.time.Duration.between(
                LocalDateTime.now(), member.lockExpiresAt
            ).toMinutes() + 1
            throw AccountLockedException("계정이 일시 잠금되었습니다. ${remainingMinutes}분 후에 다시 시도하세요.")
        }

        member.lockedAt = null
        member.lockExpiresAt = null
    }


    @Transactional
    fun createMember(req: CreateMemberReq) {
        if (memberRepository.existsByLoginId(req.loginId)) {
            throw DuplicateLoginIdException()
        }
        if (memberRepository.existsByEmail(req.email)) {
            throw DuplicateEmailException()
        }
        if (memberRepository.existsByNickname(req.nickname)) {
            throw DuplicateNicknameException()
        }
        if (memberRepository.existsByPhone(req.phone)) {
            throw DuplicatePhoneException()
        }

        val role = try {
            Role.valueOf(req.role)
        } catch (e: IllegalArgumentException) {
            Role.GUEST
        }

        val member = Member(
            loginId = req.loginId,
            password = passwordEncoder.encode(defaultPassword),
            email = req.email,
            nickname = req.nickname,
            name = req.name,
            phone = req.phone,
            role = role,
            passwordChangedAt = LocalDateTime.of(2000, 1, 1, 0, 0)
        )

        memberRepository.save(member)
    }

    @Transactional(readOnly = true)
    fun getMembers(): List<MemberRes> {
        return memberRepository.findAll().map { it.toMemberRes() }
    }

    @Transactional(readOnly = true)
    fun getProfile(loginId: String): MemberRes {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        return member.toMemberRes()
    }

    private fun Member.toMemberRes(): MemberRes {
        return MemberRes(
            id = id,
            loginId = loginId,
            email = email,
            nickname = nickname,
            name = name,
            phone = phone,
            role = role.name,
            failedLoginAttempts = failedLoginAttempts,
            lockedAt = lockedAt,
            lockExpiresAt = lockExpiresAt,
            permanentLock = lockedAt != null && lockExpiresAt == null,
            createdAt = createdAt
        )
    }

    @Transactional
    fun updateProfile(loginId: String, req: UpdateProfileReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (req.email != member.email && memberRepository.existsByEmailAndLoginIdNot(req.email, loginId)) {
            throw DuplicateEmailException()
        }

        if (req.phone != member.phone && memberRepository.existsByPhoneAndLoginIdNot(req.phone, loginId)) {
            throw DuplicatePhoneException()
        }

        member.nickname = req.nickname
        member.email = req.email
        member.name = req.name
        member.phone = req.phone

        log.info { "프로필 수정: loginId=$loginId" }
    }

    @Transactional
    fun lockAccount(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        member.lockedAt = LocalDateTime.now()
        member.lockExpiresAt = null
    }

    @Transactional
    fun unlockAccount(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        member.failedLoginAttempts = 0
        member.lockedAt = null
        member.lockExpiresAt = null
    }

    @Transactional
    fun changeRole(loginId: String, role: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        member.role = Role.valueOf(role)
    }

    data class ReissueResult(
        val tokenRes: TokenRes,
        val accessToken: String
    )

    fun reissue(refreshToken: String?): ReissueResult {
        if (refreshToken == null) {
            throw RefreshTokenMissingException()
        }

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw RefreshTokenInvalidException()
        }

        val loginId = jwtProvider.getLoginId(refreshToken)
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        return ReissueResult(
            tokenRes = TokenRes(
                role = member.role.name,
                nickname = member.nickname,
                email = maskEmail(member.email)
            ),
            accessToken = jwtProvider.generateAccessToken(loginId)
        )
    }

    @Transactional
    fun changePassword(
        loginId: String,
        req: ChangePasswordReq,
        accessToken: String? = null
    ) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (!passwordEncoder.matches(req.currentPassword, member.password)) {
            throw InvalidPasswordException()
        }

        if (req.currentPassword == req.newPassword) {
            throw SamePasswordException()
        }

        member.password = passwordEncoder.encode(req.newPassword)
        member.passwordChangedAt = LocalDateTime.now()
        jwtProvider.deleteRefreshToken(loginId)
        accessToken?.let { jwtProvider.blacklistAccessToken(it) }
    }

    fun logout(loginId: String, accessToken: String? = null) {
        jwtProvider.deleteRefreshToken(loginId)
        accessToken?.let { jwtProvider.blacklistAccessToken(it) }
    }

    @Transactional(readOnly = true)
    fun getApiKeys(loginId: String): List<ApiKeyRes> {
        return memberApiKeyRepository.findByMemberLoginId(loginId).map { it.toApiKeyRes() }
    }

    @Transactional
    fun createApiKey(loginId: String, req: ApiKeyReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (memberApiKeyRepository.existsByMemberLoginIdAndProvider(loginId, req.provider)) {
            throw DuplicateApiKeyException()
        }

        memberApiKeyRepository.save(
            MemberApiKey(
                member = member,
                provider = req.provider,
                appKey = req.appKey,
                secretKey = req.secretKey
            )
        )
    }

    @Transactional
    fun deleteApiKey(loginId: String, id: Long) {
        val apiKey = memberApiKeyRepository.findById(id)
            .orElseThrow { ApiKeyNotFoundException() }

        if (apiKey.member.loginId != loginId) {
            throw AccessDeniedException("접근 권한이 없습니다.")
        }

        memberApiKeyRepository.delete(apiKey)
    }

    private fun MemberApiKey.toApiKeyRes(): ApiKeyRes {
        return ApiKeyRes(
            id = id,
            provider = provider,
            appKey = maskApiKey(appKey),
            createdAt = createdAt
        )
    }

    private fun maskApiKey(key: String): String {
        if (key.length <= 4) return "****"
        return key.take(4) + "*".repeat(key.length - 4)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val local = parts[0]
        val domain = parts[1]
        val visible = if (local.length <= 2) 1 else 2
        return local.take(visible) + "*".repeat(local.length - visible) + "@" + domain
    }
}
