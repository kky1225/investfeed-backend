package com.example.investfeed.global.ratelimit

import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest

data class RateLimitRule(val key: String, val intervalMs: Long)

@Component
class RateLimitPolicy(
    private val props: RateLimitProperties,
) {

    fun resolve(request: ClientRequest): List<RateLimitRule> {
        val url = request.url()
        val host = url.host ?: return emptyList()
        val path = url.path ?: ""
        val authHash = request.headers().getFirst(HttpHeaders.AUTHORIZATION)?.hashCode() ?: 0

        return when (host) {
            KIWOOM_HOST -> kiwoomRules(host, authHash, request, props.kiwoomIntervalMs)
            KIWOOM_MOCK_HOST -> kiwoomRules(host, authHash, request, props.kiwoomMockIntervalMs)
            FRED_HOST -> listOf(RateLimitRule("fred", props.fredIntervalMs))
            UPBIT_HOST -> listOf(RateLimitRule("upbit:${upbitGroup(path)}", props.upbitIntervalMs))
            TOSS_HOST -> tossRule(authHash, path)?.let { listOf(it) } ?: emptyList()
            else -> emptyList()
        }
    }

    private fun kiwoomRules(host: String, authHash: Int, request: ClientRequest, intervalMs: Long): List<RateLimitRule> {
        val apiId = request.headers().getFirst("api-id") ?: request.url().path
        val rules = mutableListOf(RateLimitRule("$host:$authHash:$apiId", intervalMs))
        if (props.kiwoomPerTokenIntervalMs > 0) {
            rules += RateLimitRule("$host:$authHash", props.kiwoomPerTokenIntervalMs)
        }
        return rules
    }

    private fun upbitGroup(path: String): String =
        path.removePrefix("/v1/").substringBefore('/').ifBlank { "default" }

    private fun tossRule(authHash: Int, path: String): RateLimitRule? {
        val (group, interval) = when {
            path.startsWith("/oauth2/") -> "AUTH" to props.tossAuthIntervalMs
            path.startsWith("/api/v1/accounts") -> "ACCOUNT" to props.tossAccountIntervalMs
            path.startsWith("/api/v1/holdings") || path.startsWith("/api/v1/buying-power") -> "ASSET" to props.tossAssetIntervalMs
            path.startsWith("/api/v1/exchange-rate") -> "MARKET_INFO" to props.tossMarketInfoIntervalMs
            else -> return null
        }
        return RateLimitRule("toss:$authHash:$group", interval)
    }

    companion object {
        private const val KIWOOM_HOST = "api.kiwoom.com"
        private const val KIWOOM_MOCK_HOST = "mockapi.kiwoom.com"
        private const val FRED_HOST = "api.stlouisfed.org"
        private const val UPBIT_HOST = "api.upbit.com"
        private const val TOSS_HOST = "openapi.tossinvest.com"
    }
}
