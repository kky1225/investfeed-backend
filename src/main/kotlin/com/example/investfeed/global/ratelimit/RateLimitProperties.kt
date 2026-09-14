package com.example.investfeed.global.ratelimit

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RateLimitProperties(
    @param:Value("\${external-api.rate-limit.kiwoom-interval-ms:200}")
    val kiwoomIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.kiwoom-mock-interval-ms:1000}")
    val kiwoomMockIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.kiwoom-per-token-interval-ms:0}")
    val kiwoomPerTokenIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.fred-interval-ms:500}")
    val fredIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.upbit-interval-ms:100}")
    val upbitIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.toss-account-interval-ms:1000}")
    val tossAccountIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.toss-asset-interval-ms:200}")
    val tossAssetIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.toss-market-info-interval-ms:334}")
    val tossMarketInfoIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.toss-auth-interval-ms:200}")
    val tossAuthIntervalMs: Long,
    @param:Value("\${external-api.rate-limit.log-delays-over-ms:0}")
    val logDelaysOverMs: Long,
    @param:Value("\${external-api.rate-limit.toss-429-max-retry:3}")
    val toss429MaxRetry: Int,
)
