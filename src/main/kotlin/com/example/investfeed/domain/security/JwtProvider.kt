package com.example.investfeed.domain.security

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
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}")
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
        val now = Date()
        val refreshToken = Jwts.builder()
            .subject(loginId)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(Date(now.time + transDays(refreshTokenExpiration)))
            .signWith(key)
            .compact()

        redisTemplate.opsForValue().set(
            "RT:$loginId",
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.DAYS
        )
        return refreshToken
    }

    fun getAuthentication(token: String): Authentication {
        val loginId = getClaims(token).subject
        val userDetails = userDetailsService.loadUserByUsername(loginId)
        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = getClaims(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun validateRefreshToken(refreshToken: String): Boolean {
        if (!validateToken(refreshToken)) return false
        val loginId = getClaims(refreshToken).subject
        val stored = redisTemplate.opsForValue().get("RT:$loginId")
        return stored == refreshToken
    }

    fun getLoginId(token: String): String = getClaims(token).subject

    fun deleteRefreshToken(loginId: String) {
        redisTemplate.delete("RT:$loginId")
    }

    fun resolveToken(bearerToken: String?): String? {
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
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
