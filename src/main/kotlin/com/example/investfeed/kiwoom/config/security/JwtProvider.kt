package com.example.investfeed.kiwoom.config.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long,
    private val redisTemplate: StringRedisTemplate,
    private val userDetailsService: UserDetailsService
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(email: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(email)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpiration))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(email: String): String {
        val now = Date()
        val refreshToken = Jwts.builder()
            .subject(email)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpiration))
            .signWith(key)
            .compact()

        redisTemplate.opsForValue().set(
            "RT:$email",
            refreshToken,
            refreshTokenExpiration,
            TimeUnit.MILLISECONDS
        )
        return refreshToken
    }

    fun getAuthentication(token: String): Authentication {
        val email = getClaims(token).subject
        val userDetails = userDetailsService.loadUserByUsername(email)
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
        val email = getClaims(refreshToken).subject
        val stored = redisTemplate.opsForValue().get("RT:$email")
        return stored == refreshToken
    }

    fun getEmail(token: String): String = getClaims(token).subject

    fun deleteRefreshToken(email: String) {
        redisTemplate.delete("RT:$email")
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
}
