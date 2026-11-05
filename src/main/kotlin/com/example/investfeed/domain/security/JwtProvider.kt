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

    fun generateRefreshToken(loginId: String): String {
        val refreshToken = buildRefreshToken(loginId)
        storeRefreshToken(loginId, refreshToken)
        return refreshToken
    }

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
            "${RedisKeyPrefix.REFRESH_TOKEN.prefix}$loginId",
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.DAYS
        )
    }

    fun getStoredRefreshToken(loginId: String): String? =
        redisTemplate.opsForValue().get("${RedisKeyPrefix.REFRESH_TOKEN.prefix}$loginId")

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

    fun deleteRefreshToken(loginId: String) {
        redisTemplate.delete("${RedisKeyPrefix.REFRESH_TOKEN.prefix}$loginId")
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
