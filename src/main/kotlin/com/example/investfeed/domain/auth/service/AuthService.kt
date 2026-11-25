package com.example.investfeed.domain.auth.service

import com.example.investfeed.common.util.failure
import com.example.investfeed.domain.auth.dto.req.*
import com.example.investfeed.domain.auth.dto.res.*
import com.example.investfeed.domain.auth.entity.Member
import com.example.investfeed.domain.auth.entity.MemberApiKey
import com.example.investfeed.domain.auth.exception.*
import com.example.investfeed.domain.auth.repository.MemberApiKeyRepository
import com.example.investfeed.domain.auth.repository.MemberRepository
import com.example.investfeed.domain.auth.repository.RoleRepository
import com.example.investfeed.domain.holding.repository.BrokerRepository
import com.example.investfeed.domain.notification.config.NotificationWebSocketHandler
import com.example.investfeed.domain.permission.repository.RolePermissionRepository
import com.example.investfeed.domain.security.CustomUserDetails
import com.example.investfeed.domain.security.JwtProvider
import com.example.investfeed.global.constant.RedisKeyPrefix
import com.example.investfeed.totp.TotpService
import com.example.investfeed.toss.auth.service.TossAuthClient
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
import com.example.investfeed.kiwoom.config.WebSocketHandler
import com.example.investfeed.kiwoom.socket.KiwoomStreamClient
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

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
    private val kiwoomStreamClient: KiwoomStreamClient,
    private val webSocketHandler: WebSocketHandler,
    private val notificationWebSocketHandler: NotificationWebSocketHandler,
    private val tossAuthClient: TossAuthClient,
    private val cryptoHoldingClient: CryptoHoldingClient,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val totpService: TotpService,
    private val totpAttemptService: TotpAttemptService,
    private val secondaryPasswordAttemptService: SecondaryPasswordAttemptService,
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
            result.lockDurationSeconds?.let { throw AccountLockedByFailureException(it) }
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

        if (member.isSuspended()) throw AccountPermanentlyLockedException()

        val secret = member.totpSecret
            ?: redisTemplate.opsForValue().get("${RedisKeyPrefix.PRE_AUTH_SECRET.prefix}$preAuthToken")
            ?: throw TotpNotSetupException()

        if (!totpService.verifyCode(secret, code)) {
            val result = totpAttemptService.handleFailedTotp(loginId)
            result.lockDurationSeconds?.let {
                redisTemplate.delete("${RedisKeyPrefix.PRE_AUTH_TOKEN.prefix}$preAuthToken")
                redisTemplate.delete("${RedisKeyPrefix.PRE_AUTH_SECRET.prefix}$preAuthToken")
                throw AccountLockedByFailureException(it)
            }
            throw InvalidTotpCodeException(result.remainingAttempts)
        }

        if (member.totpSecret == null) {
            member.totpSecret = secret
        }

        totpAttemptService.resetOnSuccess(loginId)
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
            failedTotpAttempts = failedTotpAttempts,
            lockedAt = lockedAt,
            lockExpiresAt = lockExpiresAt,
            permanentLock = isSuspended(),
            totpEnabled = totpSecret != null,
            secondaryPasswordEnabled = secondaryPassword != null,
            failedSecondaryAttempts = failedSecondaryAttempts,
            secondaryPasswordSuspended = SecondaryPasswordAttemptService.isSuspended(failedSecondaryAttempts),
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

        log.debug { "프로필 수정: loginId=$loginId" }
    }

    @Transactional
    fun lockAccount(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

        member.lockedAt = LocalDateTime.now()
        member.lockExpiresAt = null

        afterCommit {
            logout(loginId)
            notificationWebSocketHandler.closeSessions(member.id)
        }
    }

    @Transactional
    fun unlockAccount(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

        member.failedLoginAttempts = 0
        member.failedTotpAttempts = 0
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

    /**
     * 관리자가 회원의 2차 비밀번호를 지운다(잊어버린 회원을 위한 것). 회원은 다음에 2차 비밀번호를 새로 만든다.
     * 회원 스스로는 현재 값을 모르면 바꿀 수 없으므로 이것이 유일한 복구 수단이다.
     * 틀려서 쌓인 횟수(정지 포함)도 0 으로 돌리고, 유지 중이던 2차 인증도 끝낸다. 정지를 푸는 방법은 이것뿐이다.
     */
    @Transactional
    fun resetSecondaryPassword(loginId: String) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }
        assertHierarchyAllowed(member)

        member.secondaryPassword = null
        // 횟수 칸은 회원 객체로는 쓰지 않는다(Member.failedSecondaryAttempts 참고). 같은 트랜잭션 안에서 쿼리로 0 을 만든다.
        memberRepository.resetSecondaryAttempts(loginId)
        // 비밀번호가 실제로 지워진(커밋된) 뒤에 끝낸다. 먼저 끝내면 그 사이 옛 비밀번호로 인증이 다시 생길 수 있다.
        afterCommit { invalidateSecondaryAuth(loginId) }
    }

    /**
     * 2차 비밀번호를 처음 만든다. 이미 있는 회원은 거부한다: 로그인만 한 상태로 현재 2차 비밀번호를 모른 채 덮어쓸 수 있으면
     * 2차 인증이 의미가 없어진다. 바꾸려면 changeSecondaryPassword(현재 값 확인)를 거쳐야 한다.
     */
    @Transactional
    fun setupSecondaryPassword(loginId: String, req: SecondaryPasswordSetupReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword != null) {
            throw SecondaryPasswordAlreadySetException()
        }

        // 예전 2차 비밀번호로 받아 둔 인증이 남아 있으면 끝낸다(관리자가 초기화한 뒤 새로 만드는 경우). 새 값으로 다시 인증해야 한다.
        invalidateSecondaryAuth(loginId)
        member.secondaryPassword = passwordEncoder.encode(req.password)
    }

    /**
     * 트랜잭션으로 묶지 않는다. 대조(checkSecondaryPassword)가 횟수를 따로 저장하느라 DB 연결을 하나 더 쓰는데,
     * 여기서 연결을 잡은 채 부르면 요청 하나가 연결 둘을 차지한다(동시에 여러 번 보내면 연결이 바닥난다).
     * 대조를 먼저 끝내고 새 값 저장만 save 의 짧은 트랜잭션으로 한다.
     */
    fun changeSecondaryPassword(loginId: String, req: SecondaryPasswordChangeReq) {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword == null) {
            throw SecondaryPasswordNotSetException()
        }

        // 인증과 같은 횟수를 쓴다. 따로 두면 이 요청만 반복해 현재 2차 비밀번호를 끝없이 넣어 볼 수 있다.
        checkSecondaryPassword(member, req.currentPassword)

        if (req.currentPassword == req.newPassword) {
            throw SameSecondaryPasswordException()
        }

        member.secondaryPassword = passwordEncoder.encode(req.newPassword)
        memberRepository.save(member)
        invalidateSecondaryAuth(loginId)
    }

    /**
     * 2차 인증이 유지되는 남은 초. SecondaryAuthFilter 와 같은 기준으로 본다: 이 브라우저의 쿠키 값이 저장된 값과 같아야 인증된 것이다
     * (다른 기기에서 다시 인증하면 저장된 값이 바뀌어 이쪽은 풀린다). 인증되지 않았으면 0
     */
    fun getSecondaryAuthRemainingSeconds(loginId: String, cookieToken: String?): Long {
        if (cookieToken.isNullOrBlank()) return 0
        val key = "${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId"
        if (redisTemplate.opsForValue().get(key) != cookieToken) return 0
        return redisTemplate.getExpire(key, TimeUnit.SECONDS).coerceAtLeast(0)
    }

    fun verifySecondaryPassword(loginId: String, req: SecondaryPasswordVerifyReq): String {
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        if (member.secondaryPassword == null) {
            throw SecondaryPasswordNotSetException()
        }

        checkSecondaryPassword(member, req.password)

        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(
            "${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId",
            token,
            secondaryAuthTtl,
            TimeUnit.MINUTES
        )
        return token
    }

    /**
     * 입력한 2차 비밀번호가 맞는지 확인한다. 2차 비밀번호를 대조하는 모든 요청(인증, 변경)이 이 한 곳을 거쳐 같은 제한을 받는다.
     * 맞히지 못한 채 연달아 10번(SecondaryPasswordAttemptService.MAX_ATTEMPTS) 틀리면 2차 비밀번호가 정지되고,
     * 관리자가 초기화할 때까지 풀리지 않는다. 그 전에는 기다리는 시간 없이 계속 넣어 볼 수 있고, 맞히면 횟수가 0 이 된다.
     *
     * 시도는 대조하기 전에 먼저 센다(DB 의 UPDATE 한 번). 대조한 뒤에 세면, 요청을 한꺼번에 여러 개 보냈을 때
     * 전부 대조까지 가서 한도를 넘겨 넣어 볼 수 있다.
     */
    private fun checkSecondaryPassword(member: Member, input: String) {
        val loginId = member.loginId
        if (SecondaryPasswordAttemptService.isSuspended(member.failedSecondaryAttempts)) {
            throw SecondaryPasswordSuspendedException()
        }
        // 세지 못했으면 그 사이 한도에 닿은 것이다(동시에 들어온 요청). 대조하지 않는다.
        val attempt = secondaryPasswordAttemptService.claim(loginId) ?: throw SecondaryPasswordSuspendedException()

        if (!passwordEncoder.matches(input, member.secondaryPassword)) {
            val remaining = SecondaryPasswordAttemptService.MAX_ATTEMPTS - attempt
            if (remaining <= 0) {
                // 정지되면 유지 중이던 2차 인증도 끝낸다.
                invalidateSecondaryAuth(loginId)
                log.warn { "2차 비밀번호 정지: loginId=$loginId, 실패횟수=$attempt" }
                throw SecondaryPasswordSuspendedException()
            }
            throw InvalidSecondaryPasswordException(remaining)
        }

        secondaryPasswordAttemptService.reset(loginId)
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

        val loginId = jwtProvider.parseRefreshClaims(refreshToken)?.subject
            ?: throw RefreshTokenInvalidException()
        val member = memberRepository.findByLoginId(loginId)
            .orElseThrow { MemberNotFoundException() }

        // 정지 시 refresh 토큰이 지워져 있으므로 아래 재사용 감지로 빠져 탈취 의심 로그가 남기 전에 막는다
        if (member.isSuspended()) throw AccountPermanentlyLockedException()

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
                webSocketHandler.closeSessions(loginId)
                log.warn { "refresh token 재사용 감지 — 세션 폐기 (loginId=$loginId)" }
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
        afterCommit { webSocketHandler.closeSessions(loginId) }
    }

    fun logout(loginId: String, accessToken: String? = null) {
        jwtProvider.deleteRefreshToken(loginId)
        accessToken?.let { jwtProvider.blacklistAccessToken(it) }
        invalidateSecondaryAuth(loginId)
        webSocketHandler.closeSessions(loginId)
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
                "토스증권" -> tossAuthClient.validateApiKey(req.appKey, req.secretKey)
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

        if (apiKey.broker.name == "키움증권") {
            val appKey = apiKey.appKey
            // 같은 키를 공유하는 계정(예: 스케줄러 계정)이 남아 있으면 연결·토큰은 유지한다
            val shared = memberApiKeyRepository.findAllByBrokerId(apiKey.broker.id).any { it.id != apiKey.id && it.appKey == appKey }
            afterCommit {
                if (shared) kiwoomStreamClient.removeSubscriber(appKey, loginId) else kiwoomStreamClient.releaseKey(appKey)
            }
        }
    }

    private fun afterCommit(block: () -> Unit) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                runCatching(block).onFailure { log.failure(it) { "커밋 후 정리 실패 (실시간 연결·2차 인증 상태)" } }
            }
        })
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
