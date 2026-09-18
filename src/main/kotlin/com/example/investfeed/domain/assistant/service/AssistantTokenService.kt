package com.example.investfeed.domain.assistant.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class AssistantTokenService(
    @param:Value("\${assistant.jwt.private-key}") private val privateKeyPem: String,
    @param:Value("\${assistant.jwt.kid}") val kid: String,
    @param:Value("\${assistant.jwt.expiration-seconds:300}") private val expirationSeconds: Long,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val ISSUER = "investfeed-core"
        const val AUD_ASSISTANT = "assistant"
        const val AUD_CORE_TOOLS = "core-tools"
        const val SCOPE_CHAT = "chat"
    }

    private val privateKey: PrivateKey = loadPrivateKey(privateKeyPem)
    val publicKey: RSAPublicKey = derivePublicKey(privateKey)

    data class Issued(val token: String, val expiresAt: Instant, val kid: String)

    fun issue(memberId: Long, scope: String = SCOPE_CHAT): Issued {
        val now = Instant.now()
        val exp = now.plusSeconds(expirationSeconds)
        val token = Jwts.builder()
            .header().keyId(kid).and()
            .issuer(ISSUER)
            .audience().add(AUD_ASSISTANT).add(AUD_CORE_TOOLS).and()
            .subject(memberId.toString())
            .claim("scope", scope)
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(privateKey, Jwts.SIG.RS256)
            .compact()
        return Issued(token, exp, kid)
    }

    fun verifyForCoreTools(token: String): Claims =
        Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer(ISSUER)
            .requireAudience(AUD_CORE_TOOLS)
            .build()
            .parseSignedClaims(token)
            .payload

    fun jwks(): Map<String, Any> = mapOf(
        "keys" to listOf(
            mapOf(
                "kty" to "RSA",
                "use" to "sig",
                "alg" to "RS256",
                "kid" to kid,
                "n" to b64url(publicKey.modulus.toByteArray()),
                "e" to b64url(publicKey.publicExponent.toByteArray()),
            )
        )
    )

    private fun b64url(bytes: ByteArray): String {
        val trimmed = if (bytes.size > 1 && bytes[0] == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(trimmed)
    }

    private fun loadPrivateKey(pem: String): PrivateKey {
        val body = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("\\s".toRegex(), "")
        require(body.isNotBlank()) { "assistant.jwt.private-key 가 비어 있습니다 (PKCS#8 PEM 필요)" }
        val spec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(body))
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun derivePublicKey(key: PrivateKey): RSAPublicKey {
        val crt = key as? RSAPrivateCrtKey ?: error("RSA CRT 개인키가 아닙니다 (PKCS#8 RSA 필요)")
        return KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(crt.modulus, crt.publicExponent)) as RSAPublicKey
    }
}
