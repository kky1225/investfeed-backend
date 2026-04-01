package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.ApiKeyReq
import com.example.investfeed.domain.auth.dto.req.ChangePasswordReq
import com.example.investfeed.domain.auth.dto.req.CreateMemberReq
import com.example.investfeed.domain.auth.dto.req.LoginReq
import com.example.investfeed.domain.auth.dto.req.SecondaryPasswordChangeReq
import com.example.investfeed.domain.auth.dto.req.SecondaryPasswordSetupReq
import com.example.investfeed.domain.auth.dto.req.SecondaryPasswordVerifyReq
import com.example.investfeed.domain.auth.dto.req.SignupReq
import com.example.investfeed.domain.auth.dto.req.UpdateProfileReq
import com.example.investfeed.domain.auth.dto.res.ApiKeyRes
import com.example.investfeed.domain.auth.dto.res.MemberRes
import com.example.investfeed.domain.auth.dto.res.PreAuthRes
import com.example.investfeed.domain.auth.dto.res.TokenRes
import com.example.investfeed.domain.auth.dto.res.TotpSetupRes
import com.example.investfeed.domain.auth.entity.MemberApiKey
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.entity.Role
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.security.JwtProvider
import com.example.investfeed.domain.auth.exception.*
import com.example.investfeed.totp.TotpService
import org.springframework.security.access.AccessDeniedException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service("memberAuthService")
class AuthService(
    @param:Value("\${security.password-change-cycle}")
    private val passwordChangeCycle: Long,
    @param:Value("\${security.default-password}")
    private val defaultPassword: String,
    private val memberRepository: MemberRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val brokerRepository: BrokerRepository,
    private val loginAttemptService: LoginAttemptService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val totpService: TotpService,
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = KotlinLogging.logger {}
    private val preAuthTokenTtl = 310L // seconds (5분 10초)
    private val secondaryAuthTtl = 30L // minutes

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

    data class PreAuthResult(
        val preAuthRes: PreAuthRes,
        val preAuthToken: String
    )

    @Transactional
    fun login(req: LoginReq): PreAuthResult {
        val member = memberRepository.findByLoginId(req.loginId)
            .orElseThrow { InvalidCredentialsException() }

        checkAccountLock(member)

        if (!passwordEncoder.matches(req.password, member.password)) {
            val result = loginAttemptService.handleFailedLogin(member.loginId)
            if (result.locked) {
                if (result.lockDurationSeconds == null) {
                    throw AccountPermanentlyLockedException()
                }
                throw AccountLockedByFailureException(result.lockDurationSeconds)
            }
            throw InvalidCredentialsException()
        }

        member.failedLoginAttempts = 0
        member.lockedAt = null
        member.lockExpiresAt = null

        val preAuthToken = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "PRE_AUTH:$preAuthToken",
            member.loginId,
            preAuthTokenTtl,
            TimeUnit.SECONDS
        )

        return PreAuthResult(
            preAuthRes = PreAuthRes(
                totpRequired = true,
                totpSetupRequired = member.totpSecret == null
            ),
            preAuthToken = preAuthToken
        )
    }

    fun totpSetup(preAuthToken: String): TotpSetupRes {
        val loginId = validatePreAuthToken(preAuthToken)
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        val secret = member.totpSecret ?: totpService.generateSecret().also { newSecret ->
            redisTemplate.opsForValue().set(
                "PRE_AUTH_SECRET:$preAuthToken",
                newSecret,
                preAuthTokenTtl,
                TimeUnit.MINUTES
            )
        }

        return TotpSetupRes(
            qrCodeImage = totpService.generateQrCodeBase64(secret, loginId)
        )
    }

    @Transactional
    fun totpVerify(preAuthToken: String, code: String): LoginResult {
        val loginId = validatePreAuthToken(preAuthToken)
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        val secret = member.totpSecret
            ?: redisTemplate.opsForValue().get("PRE_AUTH_SECRET:$preAuthToken")
            ?: throw TotpNotSetupException()

        if (!totpService.verifyCode(secret, code)) {
            throw InvalidTotpCodeException()
        }

        if (member.totpSecret == null) {
            member.totpSecret = secret
        }

        redisTemplate.delete("PRE_AUTH:$preAuthToken")
        redisTemplate.delete("PRE_AUTH_SECRET:$preAuthToken")

        val passwordChangeRequired = member.passwordChangedAt
            .plusDays(passwordChangeCycle)
            .isBefore(LocalDateTime.now())

        return LoginResult(
            tokenRes = TokenRes(
                passwordChangeRequired = passwordChangeRequired,
                role = member.role.name,
                nickname = member.nickname,
                email = maskEmail(member.email),
                secondaryPasswordEnabled = member.secondaryPassword != null
            ),
            accessToken = jwtProvider.generateAccessToken(member.loginId),
            refreshToken = jwtProvider.generateRefreshToken(member.loginId)
        )
    }

    private fun validatePreAuthToken(preAuthToken: String): String {
        return redisTemplate.opsForValue().get("PRE_AUTH:$preAuthToken")
            ?: throw PreAuthTokenInvalidException()
    }

    private fun checkAccountLock(member: Member) {
        member.lockedAt ?: return

        if (member.lockExpiresAt == null) {
            throw AccountPermanentlyLockedException()
        }

        if (member.lockExpiresAt!!.isAfter(LocalDateTime.now())) {
            val remainingSeconds = java.time.Duration.between(
                LocalDateTime.now(), member.lockExpiresAt
            ).seconds
            throw AccountLockedException(remainingSeconds)
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
            totpEnabled = totpSecret != null,
            secondaryPasswordEnabled = secondaryPassword != null,
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
    fun resetTotp(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        member.totpSecret = null
    }

    @Transactional
    fun setupSecondaryPassword(loginId: String, req: SecondaryPasswordSetupReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        member.secondaryPassword = passwordEncoder.encode(req.password)
    }

    @Transactional
    fun changeSecondaryPassword(loginId: String, req: SecondaryPasswordChangeReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword == null) {
            throw SecondaryPasswordNotSetException()
        }

        if (!passwordEncoder.matches(req.currentPassword, member.secondaryPassword)) {
            throw InvalidSecondaryPasswordException()
        }

        if (req.currentPassword == req.newPassword) {
            throw SameSecondaryPasswordException()
        }

        member.secondaryPassword = passwordEncoder.encode(req.newPassword)
        invalidateSecondaryAuth(loginId)
    }

    fun getSecondaryPasswordLockStatus(loginId: String): Long {
        val lockTtl = redisTemplate.getExpire("SEC_LOCK:$loginId", TimeUnit.SECONDS)
        return if (lockTtl > 0) lockTtl else 0
    }

    fun verifySecondaryPassword(loginId: String, req: SecondaryPasswordVerifyReq): String {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword == null) {
            throw SecondaryPasswordNotSetException()
        }

        val lockKey = "SEC_LOCK:$loginId"
        val lockTtl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)
        if (lockTtl > 0) {
            throw SecondaryPasswordLockedException(lockTtl)
        }

        if (!passwordEncoder.matches(req.password, member.secondaryPassword)) {
            val failKey = "SEC_FAIL:$loginId"
            val count = redisTemplate.opsForValue().increment(failKey) ?: 1
            if (count == 1L) {
                redisTemplate.expire(failKey, 10, TimeUnit.MINUTES)
            }
            if (count >= 5) {
                redisTemplate.opsForValue().set(lockKey, "locked", 10, TimeUnit.MINUTES)
                redisTemplate.delete(failKey)
                throw SecondaryPasswordLockedException(600)
            }
            throw InvalidSecondaryPasswordException()
        }

        redisTemplate.delete("SEC_FAIL:$loginId")
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "SEC_AUTH:$loginId",
            token,
            secondaryAuthTtl,
            TimeUnit.MINUTES
        )
        return token
    }

    fun invalidateSecondaryAuth(loginId: String) {
        redisTemplate.delete("SEC_AUTH:$loginId")
    }

    fun isSecondaryAuthValid(loginId: String, token: String): Boolean {
        val stored = redisTemplate.opsForValue().get("SEC_AUTH:$loginId") ?: return false
        return stored == token
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
                email = maskEmail(member.email),
                secondaryPasswordEnabled = member.secondaryPassword != null
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
        invalidateSecondaryAuth(loginId)
    }

    fun logout(loginId: String, accessToken: String? = null) {
        jwtProvider.deleteRefreshToken(loginId)
        accessToken?.let { jwtProvider.blacklistAccessToken(it) }
        invalidateSecondaryAuth(loginId)
    }

    @Transactional(readOnly = true)
    fun getApiKeys(loginId: String): List<ApiKeyRes> {
        return memberApiKeyRepository.findByMemberLoginId(loginId).map { it.toApiKeyRes() }
    }

    @Transactional
    fun createApiKey(loginId: String, req: ApiKeyReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        val broker = brokerRepository.findById(req.brokerId)
            .orElseThrow { IllegalArgumentException("증권사를 찾을 수 없습니다.") }

        if (memberApiKeyRepository.existsByMemberLoginIdAndBrokerId(loginId, req.brokerId)) {
            throw DuplicateApiKeyException()
        }

        memberApiKeyRepository.save(
            MemberApiKey(
                member = member,
                broker = broker,
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
            brokerId = broker.id,
            brokerName = broker.name,
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
