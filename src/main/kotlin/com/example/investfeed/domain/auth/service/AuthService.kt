package com.example.investfeed.domain.auth.service

import com.example.investfeed.domain.auth.dto.req.*
import com.example.investfeed.domain.auth.dto.res.*
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.entity.MemberApiKey
import com.example.investfeed.domain.auth.exception.*
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.auth.repository.RoleRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.permission.repository.RolePermissionRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.security.JwtProvider
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.totp.TotpService
import com.example.investfeed.upbit.holding.client.CryptoHoldingClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import com.example.investfeed.kiwoom.auth.service.AuthClient as KiwoomAuthClient

@Service("memberAuthService")
class AuthService(
    @param:Value("\${security.password-change-cycle}")
    private val passwordChangeCycle: Long,
    @param:Value("\${security.default-password}")
    private val defaultPassword: String,
    private val memberRepository: MemberRepository,
    private val memberApiKeyRepository: MemberApiKeyRepository,
    private val roleRepository: RoleRepository,
    private val rolePermissionRepository: RolePermissionRepository,
    private val brokerRepository: BrokerRepository,
    private val loginAttemptService: LoginAttemptService,
    private val apiKeyAttemptService: ApiKeyAttemptService,
    private val kiwoomAuthClient: KiwoomAuthClient,
    private val cryptoHoldingClient: CryptoHoldingClient,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val totpService: TotpService,
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = KotlinLogging.logger {}
    private val preAuthTokenTtlSetup = 610L
    private val preAuthTokenTtlVerify = 310L
    private val secondaryAuthTtl = 30L
    private val refreshGraceSeconds = 60L

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

        // TOTP 미등록 사용자는 등록 흐름(10분), 기등록 사용자는 인증 흐름(5분) 으로 TTL 구분
        val isSetupRequired = member.totpSecret == null
        val ttl = if (isSetupRequired) preAuthTokenTtlSetup else preAuthTokenTtlVerify

        val preAuthToken = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "${RedisKeyPrefix.PRE_AUTH_TOKEN.prefix}$preAuthToken",
            member.loginId,
            ttl,
            TimeUnit.SECONDS
        )

        return PreAuthResult(
            preAuthRes = PreAuthRes(
                totpRequired = true,
                totpSetupRequired = isSetupRequired
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
                "${RedisKeyPrefix.PRE_AUTH_SECRET.prefix}$preAuthToken",
                newSecret,
                preAuthTokenTtlSetup,
                TimeUnit.SECONDS
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
            ?: redisTemplate.opsForValue().get("${RedisKeyPrefix.PRE_AUTH_SECRET.prefix}$preAuthToken")
            ?: throw TotpNotSetupException()

        if (!totpService.verifyCode(secret, code)) {
            throw InvalidTotpCodeException()
        }

        if (member.totpSecret == null) {
            member.totpSecret = secret
        }

        redisTemplate.delete("${RedisKeyPrefix.PRE_AUTH_TOKEN.prefix}$preAuthToken")
        redisTemplate.delete("${RedisKeyPrefix.PRE_AUTH_SECRET.prefix}$preAuthToken")

        val passwordChangeRequired = member.passwordChangedAt
            .plusDays(passwordChangeCycle)
            .isBefore(LocalDateTime.now())

        return LoginResult(
            tokenRes = TokenRes(
                passwordChangeRequired = passwordChangeRequired,
                role = member.role.code,
                nickname = member.nickname,
                email = maskEmail(member.email),
                secondaryPasswordEnabled = member.secondaryPassword != null,
                defaultPath = member.role.defaultLandingPath,
                permissions = loadUserPermissions(member.role.id)
            ),
            accessToken = jwtProvider.generateAccessToken(member.loginId),
            refreshToken = jwtProvider.generateRefreshToken(member.loginId)
        )
    }

    private fun loadUserPermissions(roleId: Long): List<UserPermissionRes> {
        return rolePermissionRepository.findAllByRoleId(roleId)
            .groupBy { it.permission.code }
            .map { (code, grants) ->
                UserPermissionRes(
                    code = code,
                    actions = grants.map { it.action }.distinct().sorted()
                )
            }
            .sortedBy { it.code }
    }

    private fun validatePreAuthToken(preAuthToken: String): String {
        return redisTemplate.opsForValue().get("${RedisKeyPrefix.PRE_AUTH_TOKEN.prefix}$preAuthToken")
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

        val role = roleRepository.findByCode(req.role)
            ?: throw IllegalArgumentException("존재하지 않는 권한입니다: ${req.role}")

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
        return memberRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt"))
            .map { it.toMemberRes() }
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
            role = role.code,
            failedLoginAttempts = failedLoginAttempts,
            lockedAt = lockedAt,
            lockExpiresAt = lockExpiresAt,
            permanentLock = lockedAt != null && lockExpiresAt == null,
            totpEnabled = totpSecret != null,
            secondaryPasswordEnabled = secondaryPassword != null,
            apiKeyLocked = apiKeyLocked,
            createdAt = createdAt,
            permissions = loadUserPermissions(role.id)
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
        assertHierarchyAllowed(member)

        member.lockedAt = LocalDateTime.now()
        member.lockExpiresAt = null
    }

    @Transactional
    fun unlockAccount(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

        member.failedLoginAttempts = 0
        member.lockedAt = null
        member.lockExpiresAt = null
    }

    @Transactional
    fun resetTotp(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

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
        val lockTtl = redisTemplate.getExpire("${RedisKeyPrefix.SECONDARY_AUTH_LOCK.prefix}$loginId", TimeUnit.SECONDS)
        return if (lockTtl > 0) lockTtl else 0
    }

    fun verifySecondaryPassword(loginId: String, req: SecondaryPasswordVerifyReq): String {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword == null) {
            throw SecondaryPasswordNotSetException()
        }

        val lockKey = "${RedisKeyPrefix.SECONDARY_AUTH_LOCK.prefix}$loginId"
        val lockTtl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS)
        if (lockTtl > 0) {
            throw SecondaryPasswordLockedException(lockTtl)
        }

        if (!passwordEncoder.matches(req.password, member.secondaryPassword)) {
            val failKey = "${RedisKeyPrefix.SECONDARY_AUTH_FAIL.prefix}$loginId"
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

        redisTemplate.delete("${RedisKeyPrefix.SECONDARY_AUTH_FAIL.prefix}$loginId")
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId",
            token,
            secondaryAuthTtl,
            TimeUnit.MINUTES
        )
        return token
    }

    fun invalidateSecondaryAuth(loginId: String) {
        redisTemplate.delete("${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId")
    }

    @Transactional
    fun changeRole(loginId: String, role: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

        val newRole = roleRepository.findByCode(role)
            ?: throw IllegalArgumentException("존재하지 않는 권한입니다: $role")

        // 새 role 도 본인보다 낮은 priority 여야 함 (자기보다 상위 role 부여 차단)
        val currentPriority = currentUserRolePriority()
        if (newRole.priority <= currentPriority) {
            throw AccessDeniedException(
                "본인 (priority=$currentPriority) 과 동등 또는 상위 역할 '${newRole.code}' 로 변경할 수 없습니다."
            )
        }

        member.role = newRole
    }

    data class ReissueResult(
        val tokenRes: TokenRes,
        val accessToken: String,
        val refreshToken: String
    )

    fun reissue(refreshToken: String?): ReissueResult {
        if (refreshToken == null) {
            throw RefreshTokenMissingException()
        }

        if (!jwtProvider.isRefreshTokenSignatureValid(refreshToken)) {
            throw RefreshTokenInvalidException()
        }

        val loginId = jwtProvider.getLoginId(refreshToken)
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        val rotateKey = "${RedisKeyPrefix.REFRESH_ROTATE.prefix}$refreshToken"
        val current = jwtProvider.getStoredRefreshToken(loginId)

        val newRefreshToken: String = if (refreshToken == current) {
            val candidate = jwtProvider.buildRefreshToken(loginId)
            val won = redisTemplate.opsForValue()
                .setIfAbsent(rotateKey, candidate, refreshGraceSeconds, TimeUnit.SECONDS) ?: false
            if (won) {
                jwtProvider.storeRefreshToken(loginId, candidate)
                candidate
            } else {
                redisTemplate.opsForValue().get(rotateKey)
                    ?: jwtProvider.getStoredRefreshToken(loginId)
                    ?: throw RefreshTokenInvalidException()
            }
        } else {
            // 2) 현재 토큰은 아니지만 직전에 회전된 토큰(grace 윈도우 내) → 같은 새 토큰 반환
            // 3) grace 에도 없으면 폐기 토큰 재사용 = 탈취 의심 → 세션 폐기
            redisTemplate.opsForValue().get(rotateKey) ?: run {
                jwtProvider.deleteRefreshToken(loginId)
                log.error { "refresh token 재사용 감지 — 세션 폐기 (loginId=$loginId)" }
                throw RefreshTokenReuseDetectedException()
            }
        }

        return ReissueResult(
            tokenRes = TokenRes(
                role = member.role.code,
                nickname = member.nickname,
                email = maskEmail(member.email),
                secondaryPasswordEnabled = member.secondaryPassword != null,
                defaultPath = member.role.defaultLandingPath,
                permissions = loadUserPermissions(member.role.id)
            ),
            accessToken = jwtProvider.generateAccessToken(loginId),
            refreshToken = newRefreshToken
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

        if (member.apiKeyLocked) throw ApiKeyRegistrationLockedException()

        val broker = brokerRepository.findById(req.brokerId)
            .orElseThrow { IllegalArgumentException("증권사를 찾을 수 없습니다.") }

        if (memberApiKeyRepository.existsByMemberLoginIdAndBrokerId(loginId, req.brokerId)) {
            throw DuplicateApiKeyException()
        }

        try {
            when (broker.name) {
                "키움증권" -> kiwoomAuthClient.validateApiKey(req.appKey, req.secretKey)
                "업비트"   -> cryptoHoldingClient.validateApiKey(req.appKey, req.secretKey)
                else -> { /* 기타 broker 는 검증 미적용 */ }
            }
        } catch (e: InvalidApiKeyException) {
            apiKeyAttemptService.handleFailedRegistration(loginId)
            throw e
        }

        memberApiKeyRepository.save(
            MemberApiKey(
                member = member,
                broker = broker,
                appKey = req.appKey,
                secretKey = req.secretKey,
                expiresAt = req.expiresAt.atStartOfDay()
            )
        )
        apiKeyAttemptService.resetOnSuccess(loginId)
    }

    @Transactional
    fun unlockApiKeyRegistration(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)
        apiKeyAttemptService.unlock(loginId)
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
            expiresAt = expiresAt,
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

    private fun assertHierarchyAllowed(target: Member) {
        val currentPriority = currentUserRolePriority()
        if (target.role.priority <= currentPriority) {
            throw AccessDeniedException(
                "'${target.loginId}' 계정은 본인과 동등하거나 상위 권한이므로 변경할 수 없습니다."
            )
        }
    }

    private fun currentUserRolePriority(): Int {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("인증 정보를 찾을 수 없습니다.")
        val userDetails = auth.principal as? CustomUserDetails
            ?: throw IllegalStateException("인증 정보가 올바르지 않습니다.")
        return userDetails.member.role.priority
    }
}
