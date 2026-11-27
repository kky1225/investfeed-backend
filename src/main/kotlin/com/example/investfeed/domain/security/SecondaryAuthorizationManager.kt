package com.example.investfeed.domain.security

import com.example.investfeed.domain.ResponseCode
import com.example.investfeed.domain.auth.service.SecondaryPasswordAttemptService
import com.example.investfeed.global.constant.RedisKeyPrefix
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

/**
 * 2차 인증(2차 비밀번호) 판정. 어느 요청에 적용할지는 SecurityConfig 가 SecurityPaths 의 목록으로 정한다.
 *
 * 거부할 때는 이유를 요청 속성에 남기고, SecurityConfig 의 accessDeniedHandler 가 이유별 코드로 답한다
 * (화면은 코드로 설정 창·인증 창·정지 안내를 고른다). 로그인하지 않은 요청은 거부만 하고 이유를 남기지 않는다 → 401.
 */
@Component
class SecondaryAuthorizationManager(
    private val redisTemplate: StringRedisTemplate,
) : AuthorizationManager<RequestAuthorizationContext> {

    private val log = KotlinLogging.logger {}

    enum class Denial(val responseCode: ResponseCode) {
        NOT_SET(ResponseCode.AUTH_SECONDARY_NOT_SET),
        REQUIRED(ResponseCode.AUTH_SECONDARY_REQUIRED),
        SUSPENDED(ResponseCode.AUTH_SECONDARY_SUSPENDED),
    }

    companion object {
        const val DENIAL_ATTRIBUTE = "secondaryAuthDenial"
        private const val COOKIE_NAME = "secondaryAuthToken"
        private val GRANTED = AuthorizationDecision(true)
        private val DENIED = AuthorizationDecision(false)

        fun denialOf(request: HttpServletRequest): Denial? = request.getAttribute(DENIAL_ATTRIBUTE) as? Denial
    }

    override fun check(authentication: Supplier<Authentication>, context: RequestAuthorizationContext): AuthorizationDecision {
        val request = context.request
        // 코루틴 컨트롤러의 비동기 재진입·오류 화면 전달은 처음 요청에서 이미 확인했다
        if (request.dispatcherType != DispatcherType.REQUEST) return GRANTED

        // 로그인하지 않은 요청(익명)은 이유 없이 거부 → ExceptionTranslationFilter 가 401 로 답한다
        val userDetails = authentication.get().principal as? CustomUserDetails ?: return DENIED
        val loginId = userDetails.username
        val member = userDetails.member
        val uri = request.requestURI

        val denial = when {
            member.secondaryPassword == null -> Denial.NOT_SET
            // 여러 번 틀려 정지된 회원은 관리자가 초기화할 때까지 보호 기능에 들어갈 수 없다
            SecondaryPasswordAttemptService.isSuspended(member.failedSecondaryAttempts) -> {
                log.warn { "2차 비밀번호 정지 상태의 접근: uri=$uri, loginId=$loginId" }
                Denial.SUSPENDED
            }
            !hasSession(request, loginId, uri) -> Denial.REQUIRED
            else -> return GRANTED
        }
        request.setAttribute(DENIAL_ATTRIBUTE, denial)
        return DENIED
    }

    /** 이 브라우저의 2차 인증 쿠키가 저장된 값과 같아야 인증된 것이다 (AuthService.getSecondaryAuthRemainingSeconds 와 같은 기준) */
    private fun hasSession(request: HttpServletRequest, loginId: String, uri: String): Boolean {
        val token = request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value
        val storedToken = redisTemplate.opsForValue().get("${RedisKeyPrefix.SECONDARY_AUTH.prefix}$loginId")
        val valid = token != null && storedToken != null && storedToken == token
        if (!valid) {
            log.debug { "2차 인증 요구: uri=$uri, loginId=$loginId, cookieToken=${if (token == null) "NULL" else "present"}, redisToken=${if (storedToken == null) "NULL" else "present"}, match=${storedToken == token}" }
        }
        return valid
    }
}
