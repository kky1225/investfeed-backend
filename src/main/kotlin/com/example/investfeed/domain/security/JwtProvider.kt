package com.example.investfeed.domain.security

import com.example.investfeed.global.constant.RedisKeyPrefix
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @param:Value("\${jwt.secret}")
    private val secret: String,
    @param:Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @param:Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long,
    private val redisTemplate: StringRedisTemplate,
    private val userDetailsService: UserDetailsService
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(loginId: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(loginId)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(Date(now.time + transMinutes(accessTokenExpiration)))
            .signWith(key)
            .compact()
    }

    /** refresh 토큰이 폐기된 이유. 연장 요청에서 탈취 의심(재사용)과 구분하는 데 쓴다 */
    enum class RetireReason { LOGIN, LOGOUT }

    /** 로그인 시 새 refresh 토큰을 발급한다. 계정당 한 곳만 로그인하므로 이전 토큰은 "다른 곳 로그인으로 폐기"로 표시한다 */
    fun generateRefreshToken(loginId: String): String {
        getStoredRefreshToken(loginId)?.let { markRetired(it, RetireReason.LOGIN) }
        val refreshToken = buildRefreshToken(loginId)
        storeRefreshToken(loginId, refreshToken)
        return refreshToken
    }

    /** 폐기된 refresh 토큰이면 그 이유, 아니면 null */
    fun retireReasonOf(refreshToken: String): RetireReason? =
        redisTemplate.opsForValue().get("${RedisKeyPrefix.REFRESH_RETIRED.prefix}$refreshToken")
            ?.let { reason -> RetireReason.entries.firstOrNull { it.name == reason } }

    private fun markRetired(refreshToken: String, reason: RetireReason) {
        redisTemplate.opsForValue().set(
            "${RedisKeyPrefix.REFRESH_RETIRED.prefix}$refreshToken",
            reason.name,
            refreshTokenExpiration,
            TimeUnit.DAYS
        )
    }

    private fun refreshTokenKey(loginId: String) = "${RedisKeyPrefix.REFRESH_TOKEN.prefix}$loginId"

    fun buildRefreshToken(loginId: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(loginId)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(Date(now.time + transDays(refreshTokenExpiration)))
            .signWith(key)
            .compact()
    }

    fun storeRefreshToken(loginId: String, refreshToken: String) {
        redisTemplate.opsForValue().set(
            refreshTokenKey(loginId),
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.DAYS
        )
    }

    fun getStoredRefreshToken(loginId: String): String? =
        redisTemplate.opsForValue().get(refreshTokenKey(loginId))

    fun getAuthentication(claims: Claims): Authentication {
        val userDetails = userDetailsService.loadUserByUsername(claims.subject)
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }

    /** access 토큰을 읽어 내용을 돌려준다. 유효하지 않으면 null. 이후 처리는 토큰을 다시 읽지 않고 이 값을 쓴다 */
    fun parseAccessClaims(token: String): Claims? = parseClaims(token, "access")

    /** refresh 토큰을 읽어 내용을 돌려준다. 유효하지 않으면 null. 이후 처리는 토큰을 다시 읽지 않고 이 값을 쓴다 */
    fun parseRefreshClaims(token: String): Claims? = parseClaims(token, "refresh")

    private fun parseClaims(token: String, type: String): Claims? {
        return try {
            getClaims(token).takeIf { !it.expiration.before(Date()) && it["type"] == type }
        } catch (e: Exception) {
            null
        }
    }

    /** 로그아웃·비밀번호 변경·정지·탈취 의심 시 현재 refresh 토큰을 지우고 "로그아웃으로 폐기"로 표시한다 */
    fun deleteRefreshToken(loginId: String) {
        getStoredRefreshToken(loginId)?.let { markRetired(it, RetireReason.LOGOUT) }
        redisTemplate.delete(refreshTokenKey(loginId))
    }

    fun blacklistAccessToken(token: String) {
        val claims = getClaims(token)
        val remainingMs = claims.expiration.time - System.currentTimeMillis()
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                "${RedisKeyPrefix.BLACKLIST.prefix}$token",
                "blacklisted",
                remainingMs,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun isBlacklisted(token: String): Boolean {
        return redisTemplate.hasKey("${RedisKeyPrefix.BLACKLIST.prefix}$token")
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun transMinutes(time: Long): Long {
        return time * 60 * 1000
    }

    private fun transDays(time: Long): Long {
        return time * 24 * 60 * 60 * 1000
    }
}
